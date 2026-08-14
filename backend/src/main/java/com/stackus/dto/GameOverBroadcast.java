package com.stackus.dto;

import java.time.Instant;
import java.util.List;

/** 암호가 풀린 순간의 결과. 이때만 정답(code)을 공개한다. */
public record GameOverBroadcast(
		String type, String roomId, List<Integer> code, int turnCount, long elapsedSeconds, String solverPlayerId,
		String solverNickname, Instant endedAt) {

	public static GameOverBroadcast of(String roomId, List<Integer> code, int turnCount, long elapsedSeconds,
			String solverPlayerId, String solverNickname, Instant endedAt) {
		return new GameOverBroadcast(
				"GAME_OVER", roomId, code, turnCount, elapsedSeconds, solverPlayerId, solverNickname, endedAt);
	}
}
