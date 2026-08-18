package com.numbaseball.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방의 영구 메타데이터. 실시간 게임 상태(턴, 타워 등)는 Redis가 담당하며
 * 이 엔티티는 MySQL에 보존해야 하는 최소한의 정보만 가진다.
 */
@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

	private static final int NAME_MAX_LENGTH = 30;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false, unique = true, length = 36)
	private String roomId;

	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoomStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private Room(String roomId, String name) {
		this.roomId = roomId;
		this.name = name;
		this.status = RoomStatus.WAITING;
		this.createdAt = Instant.now();
	}

	public static Room create(String roomId, String name) {
		String trimmed = name == null ? "" : name.trim();
		String safeName = trimmed.isBlank() ? "이름 없는 게임" : trimmed;
		if (safeName.length() > NAME_MAX_LENGTH) {
			safeName = safeName.substring(0, NAME_MAX_LENGTH);
		}
		return new Room(roomId, safeName);
	}

	public void finish() {
		this.status = RoomStatus.FINISHED;
	}
}
