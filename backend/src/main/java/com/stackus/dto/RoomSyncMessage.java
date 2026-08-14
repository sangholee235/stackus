package com.stackus.dto;

import java.util.List;

/** 연결 직후 해당 세션에만 전송해 현재 방 상태를 복구시킨다. secretCode는 포함하지 않는다. */
public record RoomSyncMessage(String type, String roomId, String status, List<Integer> guess, int turnCount) {

	public static RoomSyncMessage of(String roomId, String status, List<Integer> guess, int turnCount) {
		return new RoomSyncMessage("SYNC", roomId, status, guess, turnCount);
	}
}
