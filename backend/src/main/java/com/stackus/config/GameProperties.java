package com.stackus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.game")
public record GameProperties(
		/** 암호 자릿수. */
		int digitCount,
		/** 자리 하나가 가질 수 있는 값의 범위 [0, digitBase). 10이면 0~9. */
		int digitBase,
		long lockTimeoutMillis) {
}
