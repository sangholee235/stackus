package com.stackus.repository;

import com.stackus.domain.Room;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Optional<Room> findByRoomId(String roomId);
}
