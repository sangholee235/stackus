package com.stackus.dto;

import java.util.List;

/** 방 전체에 브로드캐스트되는 다이얼 상태 갱신. */
public record GuessUpdateBroadcast(
		String type, String roomId, List<Integer> guess, int turnCount, String actorNickname, int digitIndex,
		String direction) {

	public static GuessUpdateBroadcast of(String roomId, List<Integer> guess, int turnCount, String actorNickname,
			int digitIndex, String direction) {
		return new GuessUpdateBroadcast("GUESS_UPDATED", roomId, guess, turnCount, actorNickname, digitIndex, direction);
	}
}
