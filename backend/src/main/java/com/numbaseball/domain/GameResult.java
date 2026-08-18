package com.numbaseball.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 암호가 풀린 시점의 최종 기록. 실시간 상태(Redis)와 분리된 영구 보존용 데이터.
 * roomName은 Room에서 그대로 복사해온 것이다 (조회 시 조인을 피하기 위한 의도적 비정규화).
 */
@Entity
@Table(name = "game_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false, length = 36)
	private String roomId;

	@Column(name = "room_name", nullable = false, length = 30)
	private String roomName;

	@Column(name = "guess_count", nullable = false)
	private int guessCount;

	/** 방이 만들어진 시각부터 암호가 풀린 시각까지, 실제 흐른 시간(초). */
	@Column(name = "elapsed_seconds", nullable = false)
	private long elapsedSeconds;

	@Column(name = "participant_count", nullable = false)
	private int participantCount;

	/** 정답을 맞춘 마지막 조작을 한 사람. */
	@Column(name = "solver_player_id", length = 36)
	private String solverPlayerId;

	@Column(name = "solver_nickname", length = 20)
	private String solverNickname;

	@Column(name = "ended_at", nullable = false)
	private Instant endedAt;

	private GameResult(String roomId, String roomName, int guessCount, long elapsedSeconds, int participantCount,
			String solverPlayerId, String solverNickname) {
		this.roomId = roomId;
		this.roomName = roomName;
		this.guessCount = guessCount;
		this.elapsedSeconds = elapsedSeconds;
		this.participantCount = participantCount;
		this.solverPlayerId = solverPlayerId;
		this.solverNickname = solverNickname;
		this.endedAt = Instant.now();
	}

	public static GameResult create(String roomId, String roomName, int guessCount, long elapsedSeconds,
			int participantCount, String solverPlayerId, String solverNickname) {
		return new GameResult(roomId, roomName, guessCount, elapsedSeconds, participantCount, solverPlayerId,
				solverNickname);
	}
}
