package com.numbaseball.redis;

public class RoomLockAcquisitionException extends RuntimeException {

	public RoomLockAcquisitionException(String roomId) {
		super("Failed to acquire lock for room: " + roomId);
	}
}
