package com.stackus.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * room:{roomId} 키에 저장되는 실시간 게임 상태.
 * MySQL에는 저장하지 않으며, 여러 Backend 인스턴스가 이 값을 공유한다.
 *
 * <p>secretCode는 절대 클라이언트에 보내지 않는다(다이얼이 맞을 때만 결과로 공개한다).
 * currentGuess는 모두가 실시간으로 보는 공유 다이얼 상태 — 위치/형태 같은 기하학적
 * 값이 전혀 없는 정수 배열이라 서버 판정과 화면이 어긋날 여지가 없다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomState {

	private String roomId;
	private String status;
	private List<Integer> secretCode = new ArrayList<>();
	private List<Integer> currentGuess = new ArrayList<>();
	private int turnCount;
	private Map<String, String> nicknamesByPlayer = new HashMap<>();
	/** 다이얼을 한 번이라도 조작한 플레이어 — 참가자 수 집계용. */
	private Set<String> actorIds = new HashSet<>();

	public static RoomState initial(String roomId, List<Integer> secretCode, List<Integer> startingGuess) {
		return new RoomState(roomId, "WAITING", secretCode, startingGuess, 0, new HashMap<>(), new HashSet<>());
	}
}
