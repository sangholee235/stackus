package com.stackus.websocket;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 이 Backend 인스턴스에 붙어있는 방별 WebSocket 세션 목록.
 * authoritative 게임 상태가 아니라 로컬 브로드캐스트 대상만 관리하며,
 * 여러 인스턴스로 수평 확장 시에는 Redis Pub/Sub으로 대체/보완한다.
 */
@Component
public class RoomSessionRegistry {

	private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();

	public void add(String roomId, WebSocketSession session) {
		sessionsByRoom.computeIfAbsent(roomId, id -> new CopyOnWriteArraySet<>()).add(session);
	}

	public void remove(String roomId, WebSocketSession session) {
		Set<WebSocketSession> sessions = sessionsByRoom.get(roomId);
		if (sessions != null) {
			sessions.remove(session);
		}
	}

	public Collection<WebSocketSession> sessionsOf(String roomId) {
		return sessionsByRoom.getOrDefault(roomId, Set.of());
	}
}
