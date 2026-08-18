package com.numbaseball.service;

import com.numbaseball.config.GameProperties;
import com.numbaseball.domain.GameResult;
import com.numbaseball.domain.Room;
import com.numbaseball.redis.RoomState;
import com.numbaseball.redis.RoomStateStore;
import com.numbaseball.repository.GameResultRepository;
import com.numbaseball.repository.RoomRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
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
		roomStateStore.save(RoomState.initial(roomId, randomSecretCode()));
		return room;
	}

	/**
	 * 서로 다른 숫자로 이루어진 비밀 코드. 숫자야구는 중복 없는 조합을 전제로 해야
	 * 스트라이크/볼 판정이 애매해지지 않는다.
	 */
	private List<Integer> randomSecretCode() {
		List<Integer> pool = new ArrayList<>(IntStream.range(0, gameProperties.digitBase()).boxed().toList());
		Collections.shuffle(pool, random);
		return List.copyOf(pool.subList(0, gameProperties.digitCount()));
	}

	@Transactional(readOnly = true)
	public Room getRoom(String roomId) {
		return roomRepository.findByRoomId(roomId)
				.orElseThrow(() -> new RoomNotFoundException(roomId));
	}

	/**
	 * 게임 종료 시 Room 상태를 MySQL에 반영하고 최종 기록을 남긴다.
	 * 방이 만들어진 시각부터 지금까지 실제로 흐른 시간(초)을 계산해 함께 저장하고 돌려준다 —
	 * 동시 접속 여부와 무관하게 아무 때나 이어서 풀 수 있는 게임이라, "몇 번 만에"뿐 아니라
	 * "실제로 며칠/몇 시간 걸렸는지"도 의미 있는 기록이기 때문이다.
	 */
	@Transactional
	public long finishGame(String roomId, int guessCount, int participantCount, String solverPlayerId,
			String solverNickname) {
		Room room = getRoom(roomId);
		room.finish();
		long elapsedSeconds = Duration.between(room.getCreatedAt(), Instant.now()).getSeconds();
		gameResultRepository.save(GameResult.create(
				roomId, room.getName(), guessCount, elapsedSeconds, participantCount, solverPlayerId, solverNickname));
		return elapsedSeconds;
	}

	@Transactional(readOnly = true)
	public GameResult getResult(String roomId) {
		return gameResultRepository.findByRoomId(roomId)
				.orElseThrow(() -> new GameResultNotFoundException(roomId));
	}

	@Transactional(readOnly = true)
	public List<GameResult> getLeaderboard() {
		return gameResultRepository.findTop20ByOrderByGuessCountAsc();
	}
}
