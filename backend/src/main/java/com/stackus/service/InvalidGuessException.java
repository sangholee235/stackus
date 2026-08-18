package com.stackus.service;

/**
 * 추측이 규칙에 맞지 않는 경우. reason은 클라이언트가 사람이 읽을 문장으로 바꿔 보여준다.
 */
public class InvalidGuessException extends RuntimeException {

	public enum Reason {
		/** 자릿수가 맞지 않는다. */
		LENGTH,
		/** 허용 범위를 벗어난 숫자가 있다. */
		RANGE,
		/** 같은 숫자를 두 번 이상 썼다. */
		DUPLICATE,
		/** 이미 누군가 똑같이 시도한 조합이다. */
		ALREADY_TRIED
	}

	private final Reason reason;

	public InvalidGuessException(Reason reason) {
		super("Invalid guess: " + reason);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
