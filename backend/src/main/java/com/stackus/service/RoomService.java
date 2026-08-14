package com.stackus.service;

import com.stackus.config.GameProperties;
import com.stackus.domain.GameResult;
import com.stackus.domain.Room;
import com.stackus.redis.RoomState;
import com.stackus.redis.RoomStateStore;
import com.stackus.repository.GameResultRepository;
import com.stackus.repository.RoomRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

	private final RoomRepository roomRepository;
	private final GameResultRepository gameResultRepository;
	private final RoomStateStore roomStateStore;
	private final GameProperties gameProperties;
	private final Random random = new Random();

	public RoomService(RoomRepository roomRepository, GameResultRepository gameResultRepository,
			RoomStateStore roomStateStore, GameProperties gameProperties) {
		this.roomRepository = roomRepository;
		this.gameResultRepository = gameResultRepository;
		this.roomStateStore = roomStateStore;
		this.gameProperties = gameProperties;
	}

	@Transactional
	public Room createRoom(String name) {
		String roomId = UUID.randomUUID().toString();
		Room room = roomRepository.save(Room.create(roomId, name));
		List<Integer> secretCode = randomSecretCode();
		List<Integer> startingGuess = GameService.zeroedDigits(gameProperties.digitCount());
		roomStateStore.save(RoomState.initial(roomId, secretCode, startingGuess));
		return room;
	}

	/** 시작 다이얼(전부 0)과 곧바로 같아지지 않도록, 전부 0인 코드는 다시 뽑는다. */
	private List<Integer> randomSecretCode() {
		List<Integer> code;
		do {
			code = new ArrayList<>();
			for (int i = 0; i < gameProperties.digitCount(); i++) {
				code.add(random.nextInt(gameProperties.digitBase()));
			}
		} while (code.stream().allMatch(digit -> digit == 0));
		return code;
	}

	@Transactional(readOnly = true)
	public Room getRoom(String roomId) {
		return roomRepository.findByRoomId(roomId)
				.orElseThrow(() -> new RoomNotFoundException(roomId));
	}

	/**
	 * 게임 종료 시 Room 상태를 MySQL에 반영하고 최종 기록을 남긴다.
	 * 방이 만들어진 시각부터 지금까지 실제로 흐른 시간(초)을 계산해 함께 저장하고 돌려준다 —
	 * 동시 접속 여부와 무관하게 방문할 수 있는 게임이라, "몇 턴"뿐 아니라 "실제로 며칠/몇
	 * 시간 걸렸는지"도 의미 있는 기록이기 때문이다.
	 */
	@Transactional
	public long finishGame(String roomId, int turnCount, int participantCount, String solverPlayerId,
			String solverNickname) {
		Room room = getRoom(roomId);
		room.finish();
		long elapsedSeconds = Duration.between(room.getCreatedAt(), Instant.now()).getSeconds();
		gameResultRepository.save(GameResult.create(
				roomId, room.getName(), turnCount, elapsedSeconds, participantCount, solverPlayerId, solverNickname));
		return elapsedSeconds;
	}

	@Transactional(readOnly = true)
	public GameResult getResult(String roomId) {
		return gameResultRepository.findByRoomId(roomId)
				.orElseThrow(() -> new GameResultNotFoundException(roomId));
	}

	@Transactional(readOnly = true)
	public List<GameResult> getLeaderboard() {
		return gameResultRepository.findTop20ByOrderByTurnCountAsc();
	}
}
