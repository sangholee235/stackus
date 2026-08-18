package com.numbaseball.service;

public class GameResultNotFoundException extends RuntimeException {

	public GameResultNotFoundException(String roomId) {
		super("Game result not found for room: " + roomId);
	}
}
