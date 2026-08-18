package com.stackus.dto;

import com.stackus.domain.GameResult;
import java.time.Instant;

public record GameResultResponse(
		String roomId, String roomName, int guessCount, long elapsedSeconds, int participantCount,
		String solverNickname, Instant endedAt) {

	public static GameResultResponse from(GameResult result) {
		return new GameResultResponse(result.getRoomId(), result.getRoomName(), result.getGuessCount(),
				result.getElapsedSeconds(), result.getParticipantCount(), result.getSolverNickname(),
				result.getEndedAt());
	}
}
