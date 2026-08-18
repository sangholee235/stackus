package com.numbaseball.repository;

import com.numbaseball.domain.Room;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Optional<Room> findByRoomId(String roomId);
}
