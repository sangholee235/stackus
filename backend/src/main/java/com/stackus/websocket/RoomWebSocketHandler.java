package com.stackus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackus.dto.AdjustMessage;
import com.stackus.dto.PresenceMessage;
import com.stackus.dto.RoomSyncMessage;
import com.stackus.dto.TurnRejectedMessage;
import com.stackus.redis.RoomLockAcquisitionException;
import com.stackus.redis.RoomState;
import com.stackus.redis.RoomStateStore;
import com.stackus.service.ActionResult;
import com.stackus.service.GameFinishedException;
import com.stackus.service.GameService;
import com.stackus.service.InvalidActionException;
import com.stackus.service.RoomNotFoundException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 연결/메시지 라우팅만 담당하고 실제 게임 규칙 판정은 GameService에 위임한다.
 */
@Slf4j
@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {

	private final RoomSessionRegistry sessionRegistry;
	private final GameService gameService;
	private final RoomStateStore roomStateStore;
	private final ObjectMapper objectMapper;

	public RoomWebSocketHandler(RoomSessionRegistry sessionRegistry, GameService gameService,
			RoomStateStore roomStateStore, ObjectMapper objectMapper) {
		this.sessionRegistry = sessionRegistry;
		this.gameService = gameService;
		this.roomStateStore = roomStateStore;
		this.objectMapper = objectMapper;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws IOException {
		String roomId = roomId(session);
		String playerId = playerId(session);
		sessionRegistry.add(roomId, session);
		log.info("WebSocket connected: room={} player={}", roomId, playerId);

		roomStateStore.find(roomId).ifPresentOrElse(state -> {
			state.getNicknamesByPlayer().put(playerId, nickname(session));
			roomStateStore.save(state);
			sendSync(session, state);
		}, () -> {
			// 상태가 없으면(만료됐거나 없는 방) 예전에는 아무것도 안 보냈다. 그러면 클라이언트는
			// 정상 연결된 줄 알고 000짜리 다이얼을 그린 채 조작만 계속 씹히는, 원인을 알 수 없는
			// 화면이 된다. 이제 명시적으로 알려서 사용자가 상황을 알 수 있게 한다.
			log.info("Room state not found on connect: room={}", roomId);
			sendTo(roomId, session, TurnRejectedMessage.of("ROOM_NOT_FOUND", null));
		});
		broadcastPresence(roomId);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		String roomId = roomId(session);
		sessionRegistry.remove(roomId, session);
		log.info("WebSocket disconnected: room={} player={}", roomId, playerId(session));
		broadcastPresence(roomId);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String roomId = roomId(session);
		String playerId = playerId(session);

		AdjustMessage payload = objectMapper.readValue(message.getPayload(), AdjustMessage.class);
		try {
			ActionResult result = gameService.act(roomId, playerId, payload);
			broadcast(roomId, result.finished() ? result.gameOver() : result.updated());
		} catch (InvalidActionException e) {
			sendTo(roomId, session, TurnRejectedMessage.of("INVALID_ACTION", null));
		} catch (GameFinishedException e) {
			sendTo(roomId, session, TurnRejectedMessage.of("GAME_FINISHED", null));
		} catch (RoomNotFoundException e) {
			sendTo(roomId, session, TurnRejectedMessage.of("ROOM_NOT_FOUND", null));
		} catch (RoomLockAcquisitionException e) {
			sendTo(roomId, session, TurnRejectedMessage.of("SERVER_BUSY", null));
		}
	}

	private void sendSync(WebSocketSession session, RoomState state) {
		sendTo(state.getRoomId(), session, RoomSyncMessage.of(state.getRoomId(), state.getStatus(),
				state.getCurrentGuess(), state.getTurnCount()));
	}

	private void broadcastPresence(String roomId) {
		broadcast(roomId, PresenceMessage.of(roomId, sessionRegistry.sessionsOf(roomId).size()));
	}

	private void broadcast(String roomId, Object payload) {
		try {
			TextMessage message = new TextMessage(objectMapper.writeValueAsString(payload));
			for (WebSocketSession session : sessionRegistry.sessionsOf(roomId)) {
				if (session.isOpen()) {
					session.sendMessage(message);
				}
			}
		} catch (IOException e) {
			log.error("Failed to broadcast to room {}", roomId, e);
		}
	}

	/**
	 * 특정 세션에게만 전송한다. 반드시 레지스트리에 보관된(동시성 안전하게 감싼) 세션으로
	 * 보내야, 다른 스레드의 브로드캐스트와 전송이 겹쳐 연결이 깨지지 않는다.
	 */
	private void sendTo(String roomId, WebSocketSession session, Object payload) {
		WebSocketSession target = sessionRegistry.find(roomId, session).orElse(session);
		try {
			target.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
		} catch (IOException e) {
			log.error("Failed to send message to session {}", session.getId(), e);
		}
	}

	private String roomId(WebSocketSession session) {
		return (String) session.getAttributes().get(RoomHandshakeInterceptor.ATTR_ROOM_ID);
	}

	private String playerId(WebSocketSession session) {
		return (String) session.getAttributes().get(RoomHandshakeInterceptor.ATTR_PLAYER_ID);
	}

	private String nickname(WebSocketSession session) {
		return (String) session.getAttributes().get(RoomHandshakeInterceptor.ATTR_NICKNAME);
	}
}
