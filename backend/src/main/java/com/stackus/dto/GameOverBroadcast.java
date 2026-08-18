package com.stackus.dto;

import com.stackus.redis.GuessRecord;
import java.time.Instant;
import java.util.List;

/** 정답을 맞힌 순간의 결과. 이때만 정답(code)을 공개한다. */
public record GameOverBroadcast(
		String type, String roomId, List<Integer> code, GuessRecord winningGuess, int guessCount,
		long elapsedSeconds, String solverPlayerId, String solverNickname, Instant endedAt) {

	public static GameOverBroadcast of(String roomId, List<Integer> code, GuessRecord winningGuess, int guessCount,
			long elapsedSeconds, String solverPlayerId, String solverNickname, Instant endedAt) {
		return new GameOverBroadcast("GAME_OVER", roomId, code, winningGuess, guessCount, elapsedSeconds,
				solverPlayerId, solverNickname, endedAt);
	}
}
