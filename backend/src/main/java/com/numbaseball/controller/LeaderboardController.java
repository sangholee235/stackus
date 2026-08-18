package com.numbaseball.controller;

import com.numbaseball.dto.LeaderboardEntryResponse;
import com.numbaseball.service.RoomService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 방을 가리지 않고 역대 최고 높이를 보여주는 글로벌 순위표. */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

	private final RoomService roomService;

	public LeaderboardController(RoomService roomService) {
		this.roomService = roomService;
	}

	@GetMapping
	public List<LeaderboardEntryResponse> getLeaderboard() {
		return roomService.getLeaderboard().stream().map(LeaderboardEntryResponse::from).toList();
	}
}
