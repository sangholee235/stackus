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

		roomStateStore.find(roomId).ifPresent(state -> {
			state.getNicknamesByPlayer().put(playerId, nickname(session));
			roomStateStore.save(state);
			sendSync(session, state);
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
			sendTo(session, TurnRejectedMessage.of("INVALID_ACTION", null));
		} catch (GameFinishedException e) {
			sendTo(session, TurnRejectedMessage.of("GAME_FINISHED", null));
		} catch (RoomNotFoundException e) {
			sendTo(session, TurnRejectedMessage.of("ROOM_NOT_FOUND", null));
		} catch (RoomLockAcquisitionException e) {
			sendTo(session, TurnRejectedMessage.of("SERVER_BUSY", null));
		}
	}

	private void sendSync(WebSocketSession session, RoomState state) {
		sendTo(session, RoomSyncMessage.of(state.getRoomId(), state.getStatus(), state.getCurrentGuess(),
				state.getTurnCount()));
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

	private void sendTo(WebSocketSession session, Object payload) {
		try {
			session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
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
