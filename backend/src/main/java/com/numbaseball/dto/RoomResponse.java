package com.numbaseball.dto;

import com.numbaseball.domain.Room;
import com.numbaseball.domain.RoomStatus;
import java.time.Instant;

public record RoomResponse(String roomId, String name, RoomStatus status, Instant createdAt) {

	public static RoomResponse from(Room room) {
		return new RoomResponse(room.getRoomId(), room.getName(), room.getStatus(), room.getCreatedAt());
	}
}
