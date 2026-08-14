package com.stackus.service;

import com.stackus.dto.GameOverBroadcast;
import com.stackus.dto.GuessUpdateBroadcast;

/**
 * GameService.act의 내부 결과. 둘 중 정확히 하나만 채워진다.
 */
public record ActionResult(GuessUpdateBroadcast updated, GameOverBroadcast gameOver) {

	public static ActionResult updated(GuessUpdateBroadcast broadcast) {
		return new ActionResult(broadcast, null);
	}

	public static ActionResult finished(GameOverBroadcast broadcast) {
		return new ActionResult(null, broadcast);
	}

	public boolean finished() {
		return gameOver != null;
	}
}
