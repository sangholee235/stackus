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
 * 여러 Backend 인스턴스가 같은 방의 다이얼을 동시에 돌리는 경우를 대비해
 * 락 기반으로 read-modify-write 구간을 직렬화한다.
 */
@Component
public class RoomStateStore {

	private static final String KEY_PREFIX = "room:";
	private static final String LOCK_PREFIX = "room-lock:";
	/**
	 * 이 게임은 "링크만 있으면 아무 때나 다시 와서 이어서 푼다"가 핵심이라, 방 상태가
	 * 몇 시간 만에 사라지면 설계 자체가 깨진다(예전엔 6시간이었다 — 하루 뒤에 들어오면
	 * 방이 그냥 없어져 있었다). 아무도 안 들어온 방을 영원히 붙들고 있을 필요는 없으니
	 * 넉넉하게 잡되 상한은 둔다. 조작이 있을 때마다 save가 호출되어 TTL이 갱신된다.
	 */
	private static final Duration STATE_TTL = Duration.ofDays(30);
	/** 락은 짧게만 잡히므로, 곧바로 실패시키지 말고 이 정도까지는 기다렸다 재시도한다. */
	private static final Duration LOCK_RETRY_INTERVAL = Duration.ofMillis(20);
	private static final int LOCK_MAX_ATTEMPTS = 40;

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

	/**
	 * roomId에 대한 락을 잡고 action을 실행한다. 락 획득에 끝내 실패하면 예외를 던진다.
	 *
	 * <p>쿨타임이 없는 게임이라 여러 명이 동시에 다이얼을 돌리는 게 정상 상황이다.
	 * 예전처럼 한 번 시도해서 실패하면 바로 거절해 버리면, 남이 같은 순간에 눌렀다는
	 * 이유만으로 내 클릭이 그냥 사라진다. 임계구역은 밀리초 단위로 짧으므로 잠깐
	 * 기다렸다 다시 잡는 편이 사용자 입장에서 훨씬 자연스럽다.
	 */
	public <T> T runWithLock(String roomId, long lockTimeoutMillis, Supplier<T> action) {
		String lockKey = LOCK_PREFIX + roomId;
		String token = UUID.randomUUID().toString();
		if (!acquire(lockKey, token, lockTimeoutMillis)) {
			throw new RoomLockAcquisitionException(roomId);
		}
		try {
			return action.get();
		} finally {
			DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
			redisTemplate.execute(script, Collections.singletonList(lockKey), token);
		}
	}

	private boolean acquire(String lockKey, String token, long lockTimeoutMillis) {
		for (int attempt = 0; attempt < LOCK_MAX_ATTEMPTS; attempt++) {
			Boolean acquired = redisTemplate.opsForValue()
					.setIfAbsent(lockKey, token, Duration.ofMillis(lockTimeoutMillis));
			if (Boolean.TRUE.equals(acquired)) {
				return true;
			}
			try {
				Thread.sleep(LOCK_RETRY_INTERVAL.toMillis());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	private String key(String roomId) {
		return KEY_PREFIX + roomId;
	}
}
