package com.stackus.config;

import com.stackus.websocket.RoomHandshakeInterceptor;
import com.stackus.websocket.RoomWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final RoomWebSocketHandler roomWebSocketHandler;

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	public WebSocketConfig(RoomWebSocketHandler roomWebSocketHandler) {
		this.roomWebSocketHandler = roomWebSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(roomWebSocketHandler, "/ws/rooms/*")
				.addInterceptors(new RoomHandshakeInterceptor())
				.setAllowedOrigins(allowedOrigins.split(","));
	}
}
