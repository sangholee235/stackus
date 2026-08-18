package com.numbaseball.websocket;

import com.numbaseball.config.PlayerIdentity;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 핸드셰이크 시점에 URL의 roomId, 쿠키의 playerId, 쿼리의 nickname을 세션 attribute로 옮겨둔다.
 * 공유 링크로 곧바로 들어와 REST를 먼저 호출한 적 없는 사용자도 있으므로,
 * 쿠키가 없으면 여기서 새로 발급해 응답 쿠키로 내려준다 (연결을 거부하지 않는다).
 */
public class RoomHandshakeInterceptor implements HandshakeInterceptor {

	public static final String ATTR_ROOM_ID = "roomId";
	public static final String ATTR_PLAYER_ID = "playerId";
	public static final String ATTR_NICKNAME = "nickname";

	private static final Pattern ROOM_ID_PATTERN = Pattern.compile("/ws/rooms/([^/]+)");
	private static final int NICKNAME_MAX_LENGTH = 20;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {
		Matcher matcher = ROOM_ID_PATTERN.matcher(request.getURI().getPath());
		if (!matcher.find()) {
			return false;
		}
		attributes.put(ATTR_ROOM_ID, matcher.group(1));

		String playerId = findCookie(request).orElse(null);
		if (playerId == null) {
			playerId = UUID.randomUUID().toString();
			issueCookie(response, playerId);
		}
		attributes.put(ATTR_PLAYER_ID, playerId);
		attributes.put(ATTR_NICKNAME, extractNickname(request));
		return true;
	}

	private String extractNickname(ServerHttpRequest request) {
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			return "익명";
		}
		// 서블릿 컨테이너가 쿼리 파라미터를 이미 URL 디코딩해서 넘겨준다.
		String raw = servletRequest.getServletRequest().getParameter("nickname");
		if (raw == null || raw.isBlank()) {
			return "익명";
		}
		return raw.length() > NICKNAME_MAX_LENGTH ? raw.substring(0, NICKNAME_MAX_LENGTH) : raw;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
		// no-op
	}

	private Optional<String> findCookie(ServerHttpRequest request) {
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			return Optional.empty();
		}
		Cookie[] cookies = servletRequest.getServletRequest().getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return java.util.Arrays.stream(cookies)
				.filter(cookie -> PlayerIdentity.COOKIE_NAME.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst();
	}

	private void issueCookie(ServerHttpResponse response, String playerId) {
		if (!(response instanceof ServletServerHttpResponse servletResponse)) {
			return;
		}
		Cookie cookie = new Cookie(PlayerIdentity.COOKIE_NAME, playerId);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(PlayerIdentity.COOKIE_MAX_AGE_SECONDS);
		servletResponse.getServletResponse().addCookie(cookie);
	}
}
