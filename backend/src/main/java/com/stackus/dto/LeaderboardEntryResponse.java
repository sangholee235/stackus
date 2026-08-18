package com.stackus.dto;

import com.stackus.domain.GameResult;
import java.time.Instant;

public record LeaderboardEntryResponse(String roomId, String roomName, int guessCount, long elapsedSeconds,
		int participantCount, String solverNickname, Instant endedAt) {

	public static LeaderboardEntryResponse from(GameResult result) {
		return new LeaderboardEntryResponse(result.getRoomId(), result.getRoomName(), result.getGuessCount(),
				result.getElapsedSeconds(), result.getParticipantCount(), result.getSolverNickname(),
				result.getEndedAt());
	}
}
