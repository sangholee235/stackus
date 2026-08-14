package com.stackus.dto;

/** 요청을 보낸 세션에만 전송한다 (방 전체 브로드캐스트 아님). */
public record TurnRejectedMessage(String type, String reason, Long remainingMillis) {

	public static TurnRejectedMessage of(String reason, Long remainingMillis) {
		return new TurnRejectedMessage("TURN_REJECTED", reason, remainingMillis);
	}
}
