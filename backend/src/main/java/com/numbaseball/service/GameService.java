package com.numbaseball.service;

import com.numbaseball.config.GameProperties;
import com.numbaseball.domain.RoomStatus;
import com.numbaseball.dto.GameOverBroadcast;
import com.numbaseball.dto.GuessAddedBroadcast;
import com.numbaseball.dto.GuessMessage;
import com.numbaseball.redis.GuessRecord;
import com.numbaseball.redis.RoomState;
import com.numbaseball.redis.RoomStateStore;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 협동 숫자야구의 authoritative 처리를 담당한다.
 *
 * <p>서버는 방마다 서로 다른 숫자로 이루어진 비밀 코드를 하나 정해두고, 누가 추측을
 * 제출하든 스트라이크(숫자와 자리가 모두 맞음)/볼(숫자는 있지만 자리가 틀림) 개수만
 * 돌려준다. 정답 자체는 맞히기 전까지 절대 내보내지 않는다.
 *
 * <p>모든 추측 기록은 방에 그대로 쌓이고 누구에게나 공개된다 — 그래서 나중에 들어온
 * 사람도 앞사람들이 뭘 시도했는지 읽고 곧바로 추리에 합류할 수 있다. 쿨타임은 없고
 * 동시 접속 여부와도 무관하다. 판정은 정수 비교뿐이라 100% 결정론적이어서, 서버가
 * 계산한 결과와 각자의 화면이 어긋날 여지가 없다.
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

	public ActionResult submitGuess(String roomId, String playerId, String nickname, GuessMessage message) {
		return roomStateStore.runWithLock(roomId, gameProperties.lockTimeoutMillis(),
				() -> doSubmitGuess(roomId, playerId, nickname, message));
	}

	private ActionResult doSubmitGuess(String roomId, String playerId, String nickname, GuessMessage message) {
		RoomState state = roomStateStore.find(roomId)
				.orElseThrow(() -> new RoomNotFoundException(roomId));

		if (RoomStatus.FINISHED.name().equals(state.getStatus())) {
			throw new GameFinishedException(roomId);
		}
		List<Integer> digits = validate(message.digits(), state);

		state.getActorIds().add(playerId);
		state.setStatus(RoomStatus.PLAYING.name());

		int strikes = countStrikes(state.getSecretCode(), digits);
		int balls = countMatchingDigits(state.getSecretCode(), digits) - strikes;
		GuessRecord record = new GuessRecord(
				UUID.randomUUID().toString(), playerId, nickname, digits, strikes, balls, Instant.now());
		state.getGuesses().add(record);

		if (strikes == gameProperties.digitCount()) {
			return ActionResult.finished(finish(state, roomId, playerId, nickname, record));
		}

		roomStateStore.save(state);
		return ActionResult.added(GuessAddedBroadcast.of(roomId, record, state.getGuesses().size()));
	}

	/** 숫자와 자리가 모두 맞은 개수. */
	private int countStrikes(List<Integer> secret, List<Integer> guess) {
		int strikes = 0;
		for (int i = 0; i < secret.size(); i++) {
			if (secret.get(i).equals(guess.get(i))) {
				strikes++;
			}
		}
		return strikes;
	}

	/** 자리와 무관하게 정답에 포함된 숫자의 개수 (스트라이크까지 포함한 값). */
	private int countMatchingDigits(List<Integer> secret, List<Integer> guess) {
		Set<Integer> secretDigits = new HashSet<>(secret);
		int matched = 0;
		for (Integer digit : guess) {
			if (secretDigits.contains(digit)) {
				matched++;
			}
		}
		return matched;
	}

	/**
	 * 규칙 위반을 구체적인 사유와 함께 걸러낸다. 이미 시도된 조합도 막는데, 기록이
	 * 모두에게 공개돼 있는 협동 게임에서 같은 조합을 다시 넣는 건 얻는 정보가 전혀 없이
	 * 시도 횟수만 늘리는 셈이기 때문이다.
	 */
	private List<Integer> validate(List<Integer> digits, RoomState state) {
		if (digits == null || digits.size() != gameProperties.digitCount()) {
			throw new InvalidGuessException(InvalidGuessException.Reason.LENGTH);
		}
		for (Integer digit : digits) {
			if (digit == null || digit < 0 || digit >= gameProperties.digitBase()) {
				throw new InvalidGuessException(InvalidGuessException.Reason.RANGE);
			}
		}
		if (new HashSet<>(digits).size() != digits.size()) {
			throw new InvalidGuessException(InvalidGuessException.Reason.DUPLICATE);
		}
		boolean alreadyTried = state.getGuesses().stream()
				.anyMatch(guess -> guess.digits().equals(digits));
		if (alreadyTried) {
			throw new InvalidGuessException(InvalidGuessException.Reason.ALREADY_TRIED);
		}
		return List.copyOf(digits);
	}

	private GameOverBroadcast finish(RoomState state, String roomId, String playerId, String nickname,
			GuessRecord winningGuess) {
		state.setStatus(RoomStatus.FINISHED.name());
		Instant endedAt = Instant.now();
		roomStateStore.save(state);

		int guessCount = state.getGuesses().size();
		long elapsedSeconds = roomService.finishGame(
				roomId, guessCount, state.getActorIds().size(), playerId, nickname);

		return GameOverBroadcast.of(roomId, state.getSecretCode(), winningGuess, guessCount, elapsedSeconds,
				playerId, nickname, endedAt);
	}
}
