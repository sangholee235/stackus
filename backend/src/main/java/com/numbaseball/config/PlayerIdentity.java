package com.numbaseball.config;

public final class PlayerIdentity {

	public static final String COOKIE_NAME = "playerId";
	public static final String REQUEST_ATTRIBUTE = "playerId";
	public static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30; // 30일

	private PlayerIdentity() {
	}
}
