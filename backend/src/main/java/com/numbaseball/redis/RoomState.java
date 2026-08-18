package com.numbaseball.redis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * room:{roomId} 키에 저장되는 실시간 게임 상태.
 * MySQL에는 저장하지 않으며, 여러 Backend 인스턴스가 이 값을 공유한다.
 *
 * <p>secretCode는 절대 클라이언트에 보내지 않는다 (정답을 맞힌 순간에만 결과로 공개한다).
 * guesses는 모두가 함께 보는 공유 추리 기록이다 — 위치나 형태 같은 기하학적 값이 전혀
 * 없는 정수 목록이라 서버 판정과 화면이 어긋날 여지가 없다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomState {

	private String roomId;
	private String status;
	private List<Integer> secretCode = new ArrayList<>();
	private List<GuessRecord> guesses = new ArrayList<>();
	/** 한 번이라도 추측을 제출한 플레이어 — 참가자 수 집계용. */
	private Set<String> actorIds = new HashSet<>();

	public static RoomState initial(String roomId, List<Integer> secretCode) {
		return new RoomState(roomId, "WAITING", secretCode, new ArrayList<>(), new HashSet<>());
	}
}
