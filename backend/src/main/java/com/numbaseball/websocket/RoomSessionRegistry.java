package com.numbaseball.websocket;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * 이 Backend 인스턴스에 붙어있는 방별 WebSocket 세션 목록.
 * authoritative 게임 상태가 아니라 로컬 브로드캐스트 대상만 관리하며,
 * 여러 인스턴스로 수평 확장 시에는 Redis Pub/Sub으로 대체/보완한다.
 *
 * <p>세션은 반드시 {@link ConcurrentWebSocketSessionDecorator}로 감싸서 보관한다.
 * WebSocketSession.sendMessage는 스레드 안전하지 않아서, 여러 사람이 동시에 다이얼을
 * 돌려 브로드캐스트가 겹치면 "The remote endpoint was in state [TEXT_PARTIAL_WRITING]"
 * 예외가 나며 그 연결이 끊어져 버린다 — 여러 명이 같이 노는 게 이 게임의 핵심이라
 * 가장 흔한 상황에서 터지는 문제였다. 데코레이터가 전송을 직렬화해 이를 막는다.
 */
@Component
public class RoomSessionRegistry {

	/** 한 번의 전송이 이 시간을 넘기면 세션을 정리한다 (느린/죽은 클라이언트 방어). */
	private static final int SEND_TIME_LIMIT_MILLIS = 5_000;
	/** 밀린 메시지가 이 크기를 넘으면 세션을 정리한다. */
	private static final int SEND_BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

	/** roomId -> (sessionId -> 동시성 안전하게 감싼 세션) */
	private final ConcurrentHashMap<String, Map<String, WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();

	public void add(String roomId, WebSocketSession session) {
		WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
				session, SEND_TIME_LIMIT_MILLIS, SEND_BUFFER_SIZE_LIMIT_BYTES);
		sessionsByRoom.computeIfAbsent(roomId, id -> new ConcurrentHashMap<>())
				.put(session.getId(), safeSession);
	}

	public void remove(String roomId, WebSocketSession session) {
		// 보관 중인 건 감싼 세션이라 객체 동일성으로는 못 지운다. 원본과 동일한 세션 id로 지운다.
		sessionsByRoom.computeIfPresent(roomId, (id, sessions) -> {
			sessions.remove(session.getId());
			// 아무도 없는 방을 계속 들고 있으면 그대로 누수가 되므로 함께 정리한다.
			return sessions.isEmpty() ? null : sessions;
		});
	}

	public Collection<WebSocketSession> sessionsOf(String roomId) {
		Map<String, WebSocketSession> sessions = sessionsByRoom.get(roomId);
		return sessions == null ? List.of() : sessions.values();
	}

	/** 특정 세션에게만 보낼 때도 감싼 세션을 써야 브로드캐스트와 전송이 겹치지 않는다. */
	public Optional<WebSocketSession> find(String roomId, WebSocketSession session) {
		Map<String, WebSocketSession> sessions = sessionsByRoom.get(roomId);
		return sessions == null ? Optional.empty() : Optional.ofNullable(sessions.get(session.getId()));
	}
}
