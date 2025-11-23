package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Room;
import com.example.quizia_backend.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerRoom(@RequestBody Room room) {
        roomRepository.addRoom(room);
        return ResponseEntity.ok().build();
    }
}
