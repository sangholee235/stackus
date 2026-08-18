package com.numbaseball.controller;

import com.numbaseball.domain.GameResult;
import com.numbaseball.domain.Room;
import com.numbaseball.dto.CreateRoomRequest;
import com.numbaseball.dto.GameResultResponse;
import com.numbaseball.dto.RoomResponse;
import com.numbaseball.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

	private final RoomService roomService;

	public RoomController(RoomService roomService) {
		this.roomService = roomService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoomResponse createRoom(@RequestBody(required = false) CreateRoomRequest request) {
		String name = request == null ? null : request.name();
		Room room = roomService.createRoom(name);
		return RoomResponse.from(room);
	}

	@GetMapping("/{roomId}")
	public RoomResponse getRoom(@PathVariable String roomId) {
		Room room = roomService.getRoom(roomId);
		return RoomResponse.from(room);
	}

	@GetMapping("/{roomId}/result")
	public GameResultResponse getResult(@PathVariable String roomId) {
		GameResult result = roomService.getResult(roomId);
		return GameResultResponse.from(result);
	}
}
