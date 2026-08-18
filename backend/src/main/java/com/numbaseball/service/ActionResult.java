package com.numbaseball.service;

import com.numbaseball.dto.GameOverBroadcast;
import com.numbaseball.dto.GuessAddedBroadcast;

/**
 * GameService.submitGuess의 내부 결과. 둘 중 정확히 하나만 채워진다.
 */
public record ActionResult(GuessAddedBroadcast added, GameOverBroadcast gameOver) {

	public static ActionResult added(GuessAddedBroadcast broadcast) {
		return new ActionResult(broadcast, null);
	}

	public static ActionResult finished(GameOverBroadcast broadcast) {
		return new ActionResult(null, broadcast);
	}

	public boolean finished() {
		return gameOver != null;
	}
}
