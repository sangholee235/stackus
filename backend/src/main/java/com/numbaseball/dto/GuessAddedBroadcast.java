package com.numbaseball.dto;

import com.numbaseball.redis.GuessRecord;

/** 방 전체에 브로드캐스트되는 새 추측 기록. */
public record GuessAddedBroadcast(String type, String roomId, GuessRecord guess, int guessCount) {

	public static GuessAddedBroadcast of(String roomId, GuessRecord guess, int guessCount) {
		return new GuessAddedBroadcast("GUESS_ADDED", roomId, guess, guessCount);
	}
}
