package com.stackus.service;

public class GameFinishedException extends RuntimeException {

	public GameFinishedException(String roomId) {
		super("Game already finished for room: " + roomId);
	}
}
