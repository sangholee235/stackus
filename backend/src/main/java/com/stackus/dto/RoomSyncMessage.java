package com.stackus.dto;

import com.stackus.redis.GuessRecord;
import java.util.List;

/** 연결 직후 해당 세션에만 전송해 현재 방 상태를 복구시킨다. secretCode는 포함하지 않는다. */
public record RoomSyncMessage(
		String type, String roomId, String status, List<GuessRecord> guesses, int digitCount) {

	public static RoomSyncMessage of(String roomId, String status, List<GuessRecord> guesses, int digitCount) {
		return new RoomSyncMessage("SYNC", roomId, status, guesses, digitCount);
	}
}
