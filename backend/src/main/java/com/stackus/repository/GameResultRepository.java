package com.stackus.repository;

import com.stackus.domain.GameResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

	Optional<GameResult> findByRoomId(String roomId);

	/** 가장 적은 횟수로 깬 순서. 턴이 같으면 리포지토리 기본 정렬(등록 순)로 처리된다. */
	List<GameResult> findTop20ByOrderByGuessCountAsc();
}
