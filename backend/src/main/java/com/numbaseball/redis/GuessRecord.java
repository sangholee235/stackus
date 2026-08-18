package com.numbaseball.redis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * 한 번의 추측과 그 판정 결과. 방에 들어온 누구나 이 기록을 보고 추리를 이어갈 수 있도록
 * 전부 보관한다 — 나중에 들어온 사람도 남들이 뭘 시도했는지 보고 바로 합류할 수 있다.
 *
 * @param strikes 숫자와 자리가 모두 맞은 개수
 * @param balls   숫자는 있지만 자리가 틀린 개수
 */
public record GuessRecord(
		String guessId,
		String playerId,
		String nickname,
		List<Integer> digits,
		int strikes,
		int balls,
		Instant guessedAt) {

	/**
	 * 스트라이크도 볼도 없는 경우. 그 숫자들은 전부 정답에 없다는 뜻이라 정보량이 크다.
	 *
	 * <p>{@code @JsonIgnore}가 반드시 필요하다. 이게 없으면 Jackson이 이 메서드를 {@code out}
	 * 이라는 프로퍼티로 보고 Redis에 함께 써버리는데, record에는 그런 구성요소가 없어서
	 * 다시 읽을 때 "Unrecognized field out"으로 역직렬화가 통째로 실패한다.
	 */
	@JsonIgnore
	public boolean isOut() {
		return strikes == 0 && balls == 0;
	}
}
