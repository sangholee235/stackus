package com.stackus.redis;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * room:{roomId} 게임 상태 저장/조회 및 동시 쓰기 방지용 분산 락.
 * 여러 Backend 인스턴스가 같은 방에 동시에 브릭을 배치하려는 경우를 대비해
 * 락 기반으로 read-modify-write 구간을 직렬화한다.
 */
@Component
public class RoomStateStore {

	private static final String KEY_PREFIX = "room:";
	private static final String LOCK_PREFIX = "room-lock:";
	private static final Duration STATE_TTL = Duration.ofHours(6);

	// 락 소유자 토큰이 일치할 때만 삭제 (다른 스레드가 획득한 락을 실수로 해제하지 않기 위함)
	private static final String UNLOCK_SCRIPT =
			"if redis.call('get', KEYS[1]) == ARGV[1] then " +
			"return redis.call('del', KEYS[1]) else return 0 end";

	private final RedisTemplate<String, Object> redisTemplate;

	public RoomStateStore(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void save(RoomState state) {
		redisTemplate.opsForValue().set(key(state.getRoomId()), state, STATE_TTL);
	}

	public Optional<RoomState> find(String roomId) {
		Object value = redisTemplate.opsForValue().get(key(roomId));
		if (value instanceof RoomState roomState) {
			return Optional.of(roomState);
		}
		return Optional.empty();
	}

	/** roomId에 대한 락을 잡고 action을 실행한다. 락 획득 실패 시 예외를 던진다. */
	public <T> T runWithLock(String roomId, long lockTimeoutMillis, Supplier<T> action) {
		String lockKey = LOCK_PREFIX + roomId;
		String token = UUID.randomUUID().toString();
		Boolean acquired = redisTemplate.opsForValue()
				.setIfAbsent(lockKey, token, Duration.ofMillis(lockTimeoutMillis));

		if (!Boolean.TRUE.equals(acquired)) {
			throw new RoomLockAcquisitionException(roomId);
		}
		try {
			return action.get();
		} finally {
			DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
			redisTemplate.execute(script, Collections.singletonList(lockKey), token);
		}
	}

	private String key(String roomId) {
		return KEY_PREFIX + roomId;
	}
}
