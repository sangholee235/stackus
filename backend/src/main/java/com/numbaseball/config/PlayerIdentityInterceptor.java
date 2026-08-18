package com.numbaseball.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 익명 playerId 발급/식별.
 * 요청 쿠키에 playerId가 없으면 UUID를 발급해 응답 쿠키로 내려주고,
 * 있으면 그대로 재사용해 재접속 시 동일 사용자로 식별한다.
 */
public class PlayerIdentityInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String playerId = findCookie(request)
				.orElseGet(() -> {
					String newId = UUID.randomUUID().toString();
					response.addCookie(createCookie(newId));
					return newId;
				});
		request.setAttribute(PlayerIdentity.REQUEST_ATTRIBUTE, playerId);
		return true;
	}

	private Optional<String> findCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return java.util.Arrays.stream(cookies)
				.filter(cookie -> PlayerIdentity.COOKIE_NAME.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst();
	}

	private Cookie createCookie(String playerId) {
		Cookie cookie = new Cookie(PlayerIdentity.COOKIE_NAME, playerId);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(PlayerIdentity.COOKIE_MAX_AGE_SECONDS);
		return cookie;
	}
}
