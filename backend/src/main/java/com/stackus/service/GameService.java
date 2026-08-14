package com.stackus.service;

import com.stackus.config.GameProperties;
import com.stackus.domain.RoomStatus;
import com.stackus.dto.AdjustMessage;
import com.stackus.dto.GameOverBroadcast;
import com.stackus.dto.GuessUpdateBroadcast;
import com.stackus.redis.RoomState;
import com.stackus.redis.RoomStateStore;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * "3자리 암호 풀기" 게임의 authoritative 처리를 담당한다.
 *
 * <p>공유 상태는 숫자 배열 하나(currentGuess)뿐이고, 행동도 "자리 하나를 +1 또는 -1"
 * 뿐이다. 위치나 형태 같은 기하학적 값이 전혀 없으므로 서버가 계산한 결과와 클라이언트
 * 화면이 어긋날 여지 자체가 없다 — 그냥 정수를 더하고 빼는 것뿐이라 100% 결정론적이다.
 *
 * <p>누구나 아무 때나(동시 접속 여부와 무관하게, 쿨타임도 없이) 링크로 들어와 다이얼을
 * 돌릴 수 있다. 별도의 "방해" 행동은 없다 — 아무나 엉뚱한 방향으로 돌리는 것 자체가
 * 자연스러운 방해다. 다이얼이 비밀 코드와 정확히 같아지는 순간 게임이 끝나고, 그때까지
 * 걸린 턴 수와 (방이 만들어진 시각 기준) 실제 시간이 함께 기록된다.
 */
@Service
public class GameService {

	private final RoomStateStore roomStateStore;
	private final RoomService roomService;
	private final GameProperties gameProperties;

	public GameService(RoomStateStore roomStateStore, RoomService roomService, GameProperties gameProperties) {
		this.roomStateStore = roomStateStore;
		this.roomService = roomService;
		this.gameProperties = gameProperties;
	}

	public ActionResult act(String roomId, String playerId, AdjustMessage message) {
		return roomStateStore.runWithLock(roomId, gameProperties.lockTimeoutMillis(),
				() -> doAct(roomId, playerId, message));
	}

	private ActionResult doAct(String roomId, String playerId, AdjustMessage message) {
		RoomState state = roomStateStore.find(roomId)
				.orElseThrow(() -> new RoomNotFoundException(roomId));

		if (RoomStatus.FINISHED.name().equals(state.getStatus())) {
			throw new GameFinishedException(roomId);
		}
		validateDigitIndex(message.digitIndex());
		Direction direction = parseDirection(message.direction());

		String nickname = state.getNicknamesByPlayer().getOrDefault(playerId, "익명");
		state.getActorIds().add(playerId);
		state.setStatus(RoomStatus.PLAYING.name());
		state.setTurnCount(state.getTurnCount() + 1);

		int delta = direction == Direction.UP ? 1 : -1;
		int base = gameProperties.digitBase();
		int current = state.getCurrentGuess().get(message.digitIndex());
		int adjusted = Math.floorMod(current + delta, base);
		state.getCurrentGuess().set(message.digitIndex(), adjusted);

		if (state.getCurrentGuess().equals(state.getSecretCode())) {
			return ActionResult.finished(finish(state, roomId, playerId, nickname));
		}

		roomStateStore.save(state);
		return ActionResult.updated(GuessUpdateBroadcast.of(
				roomId, state.getCurrentGuess(), state.getTurnCount(), nickname, message.digitIndex(),
				direction.name()));
	}

	private GameOverBroadcast finish(RoomState state, String roomId, String playerId, String nickname) {
		state.setStatus(RoomStatus.FINISHED.name());
		Instant endedAt = Instant.now();
		roomStateStore.save(state);

		long elapsedSeconds = roomService.finishGame(
				roomId, state.getTurnCount(), state.getActorIds().size(), playerId, nickname);

		return GameOverBroadcast.of(
				roomId, state.getCurrentGuess(), state.getTurnCount(), elapsedSeconds, playerId, nickname, endedAt);
	}

	private void validateDigitIndex(int digitIndex) {
		if (digitIndex < 0 || digitIndex >= gameProperties.digitCount()) {
			throw new InvalidActionException("digitIndex=" + digitIndex);
		}
	}

	private Direction parseDirection(String raw) {
		try {
			return Direction.valueOf(raw);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new InvalidActionException("direction=" + raw);
		}
	}

	/** 방 생성 시 호출되어 비밀 코드와 시작 다이얼을 뽑는다. 시작 다이얼은 항상 전부 0이다. */
	public static List<Integer> zeroedDigits(int digitCount) {
		return new java.util.ArrayList<>(java.util.Collections.nCopies(digitCount, 0));
	}
}
