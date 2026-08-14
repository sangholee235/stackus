package com.stackus.dto;

import com.stackus.domain.Room;
import com.stackus.domain.RoomStatus;
import java.time.Instant;

public record RoomResponse(String roomId, String name, RoomStatus status, Instant createdAt) {

	public static RoomResponse from(Room room) {
		return new RoomResponse(room.getRoomId(), room.getName(), room.getStatus(), room.getCreatedAt());
	}
}
