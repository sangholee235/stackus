package com.numbaseball.dto;

public record PresenceMessage(String type, String roomId, int playerCount) {

	public static PresenceMessage of(String roomId, int playerCount) {
		return new PresenceMessage("PRESENCE", roomId, playerCount);
	}
}
