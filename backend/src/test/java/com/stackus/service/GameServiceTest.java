package com.stackus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stackus.config.GameProperties;
import com.stackus.domain.RoomStatus;
import com.stackus.dto.GuessMessage;
import com.stackus.redis.GuessRecord;
import com.stackus.redis.RoomState;
import com.stackus.redis.RoomStateStore;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스트라이크/볼 판정과 규칙 검증은 이 게임의 전부라, 여기가 틀리면 게임이 성립하지
 * 않는다. 외부 의존성(Redis, DB)은 모두 스텁으로 대체하고 순수 판정 로직만 검증한다.
 */
class GameServiceTest {

	private static final String ROOM_ID = "room-1";
	private static final String PLAYER_ID = "player-1";
	private static final String NICKNAME = "테스터";

	private RoomStateStore roomStateStore;
	private RoomService roomService;
	private GameService gameService;
	private RoomState state;

	@BeforeEach
	void setUp() {
		roomStateStore = mock(RoomStateStore.class);
		roomService = mock(RoomService.class);
		GameProperties properties = new GameProperties(3, 10, 3000);
		gameService = new GameService(roomStateStore, roomService, properties);

		// 락은 그대로 통과시키고 콜백만 실행한다.
		when(roomStateStore.runWithLock(anyString(), anyLong(), any())).thenAnswer(invocation -> {
			Supplier<?> action = invocation.getArgument(2);
			return action.get();
		});
		when(roomService.finishGame(anyString(), anyInt(), anyInt(), anyString(), anyString())).thenReturn(42L);
	}

	private void givenSecret(Integer... digits) {
		state = RoomState.initial(ROOM_ID, List.of(digits));
		when(roomStateStore.find(ROOM_ID)).thenReturn(Optional.of(state));
	}

	private GuessRecord guess(Integer... digits) {
		ActionResult result = gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(digits)));
		return result.finished() ? result.gameOver().winningGuess() : result.added().guess();
	}

	@Test
	@DisplayName("자리까지 맞으면 스트라이크, 숫자만 맞으면 볼로 센다")
	void countsStrikesAndBalls() {
		givenSecret(1, 2, 3);

		GuessRecord record = guess(1, 3, 2);

		assertThat(record.strikes()).isEqualTo(1); // 1은 자리까지 일치
		assertThat(record.balls()).isEqualTo(2); // 3, 2는 숫자만 일치
	}

	@Test
	@DisplayName("겹치는 숫자가 하나도 없으면 아웃이다")
	void countsOut() {
		givenSecret(1, 2, 3);

		GuessRecord record = guess(4, 5, 6);

		assertThat(record.strikes()).isZero();
		assertThat(record.balls()).isZero();
		assertThat(record.isOut()).isTrue();
	}

	@Test
	@DisplayName("자리만 전부 틀린 경우 스트라이크 없이 볼만 센다")
	void countsOnlyBallsWhenAllMisplaced() {
		givenSecret(1, 2, 3);

		GuessRecord record = guess(3, 1, 2);

		assertThat(record.strikes()).isZero();
		assertThat(record.balls()).isEqualTo(3);
	}

	@Test
	@DisplayName("전부 맞히면 게임이 끝나고 정답과 기록이 공개된다")
	void finishesOnAllStrikes() {
		givenSecret(4, 7, 1);
		guess(1, 2, 3); // 시도 횟수가 쌓이는지 함께 확인

		ActionResult result = gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(4, 7, 1)));

		assertThat(result.finished()).isTrue();
		assertThat(result.gameOver().code()).containsExactly(4, 7, 1);
		assertThat(result.gameOver().guessCount()).isEqualTo(2);
		assertThat(result.gameOver().elapsedSeconds()).isEqualTo(42L);
		assertThat(result.gameOver().solverNickname()).isEqualTo("테스터");
		assertThat(state.getStatus()).isEqualTo(RoomStatus.FINISHED.name());
	}

	@Test
	@DisplayName("맞히기 전에는 정답을 절대 내보내지 않는다")
	void neverLeaksSecretBeforeSolving() {
		givenSecret(1, 2, 3);

		ActionResult result = gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(1, 2, 4)));

		assertThat(result.finished()).isFalse();
		assertThat(result.gameOver()).isNull();
		// 브로드캐스트에 담기는 건 제출한 추측과 판정 결과뿐이다.
		assertThat(result.added().guess().digits()).containsExactly(1, 2, 4);
	}

	@Test
	@DisplayName("자릿수가 맞지 않으면 거절한다")
	void rejectsWrongLength() {
		givenSecret(1, 2, 3);

		assertThatThrownBy(() -> gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(1, 2))))
				.isInstanceOf(InvalidGuessException.class)
				.extracting(exception -> ((InvalidGuessException) exception).getReason())
				.isEqualTo(InvalidGuessException.Reason.LENGTH);
	}

	@Test
	@DisplayName("허용 범위를 벗어난 숫자는 거절한다")
	void rejectsOutOfRange() {
		givenSecret(1, 2, 3);

		assertThatThrownBy(() -> gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(1, 2, 10))))
				.isInstanceOf(InvalidGuessException.class)
				.extracting(exception -> ((InvalidGuessException) exception).getReason())
				.isEqualTo(InvalidGuessException.Reason.RANGE);
	}

	@Test
	@DisplayName("같은 숫자를 두 번 쓰면 거절한다")
	void rejectsDuplicateDigits() {
		givenSecret(1, 2, 3);

		assertThatThrownBy(() -> gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(1, 1, 2))))
				.isInstanceOf(InvalidGuessException.class)
				.extracting(exception -> ((InvalidGuessException) exception).getReason())
				.isEqualTo(InvalidGuessException.Reason.DUPLICATE);
	}

	@Test
	@DisplayName("이미 시도된 조합은 거절한다 - 정보가 없이 횟수만 늘기 때문")
	void rejectsAlreadyTried() {
		givenSecret(1, 2, 3);
		guess(4, 5, 6);

		assertThatThrownBy(() -> gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(4, 5, 6))))
				.isInstanceOf(InvalidGuessException.class)
				.extracting(exception -> ((InvalidGuessException) exception).getReason())
				.isEqualTo(InvalidGuessException.Reason.ALREADY_TRIED);
	}

	@Test
	@DisplayName("이미 끝난 방에는 더 제출할 수 없다")
	void rejectsFinishedGame() {
		givenSecret(1, 2, 3);
		state.setStatus(RoomStatus.FINISHED.name());

		assertThatThrownBy(() -> gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(4, 5, 6))))
				.isInstanceOf(GameFinishedException.class);
	}

	@Test
	@DisplayName("없는 방이면 거절한다")
	void rejectsMissingRoom() {
		when(roomStateStore.find(ROOM_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameService.submitGuess(ROOM_ID, PLAYER_ID, NICKNAME, new GuessMessage(List.of(1, 2, 3))))
				.isInstanceOf(RoomNotFoundException.class);
	}

	@Test
	@DisplayName("추측 기록이 순서대로 쌓이고 참가자가 집계된다")
	void accumulatesHistory() {
		givenSecret(1, 2, 3);

		guess(4, 5, 6);
		guess(7, 8, 9);

		assertThat(state.getGuesses()).hasSize(2);
		assertThat(state.getGuesses().get(0).digits()).containsExactly(4, 5, 6);
		assertThat(state.getGuesses().get(1).digits()).containsExactly(7, 8, 9);
		assertThat(state.getActorIds()).containsExactly(PLAYER_ID);
		assertThat(state.getStatus()).isEqualTo(RoomStatus.PLAYING.name());
	}
}
