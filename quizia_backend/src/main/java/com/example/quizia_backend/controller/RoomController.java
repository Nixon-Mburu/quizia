package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Room;
import com.example.quizia_backend.repository.RoomRepository;
import com.example.quizia_backend.repository.RoomMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;

    public RoomController(RoomRepository roomRepository, RoomMemberRepository memberRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public List<Room> listRooms() {
        List<Room> rooms = roomRepository.findAll();
        // enrich rooms with live member counts/names from room_members
        for (Room r : rooms) {
            try {
                int count = memberRepository.countMembers(r.getRoomId());
                r.setMemberCount(count);
                java.util.List<java.util.Map<String,Object>> members = memberRepository.listMembers(r.getRoomId());
                StringBuilder names = new StringBuilder();
                for (java.util.Map<String,Object> m : members) {
                    if (names.length() > 0) names.append(",");
                    names.append(java.util.Objects.toString(m.get("username"), ""));
                }
                r.setMemberNames(names.toString());
            } catch (Exception ex) {
                // ignore and leave values previously set by repository
            }
        }
        return rooms;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerRoom(@RequestBody Room room) {
        if (room == null) return ResponseEntity.badRequest().body("room payload required");
        if (room.getRoomId() == null || room.getRoomId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("roomId required");
        }
        if (room.getRoomName() == null || room.getRoomName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("roomName required");
        }
        try {
            roomRepository.addRoom(room);
            return ResponseEntity.status(201).build();
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("could not register room: " + ex.getMessage());
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> startRoom(@RequestBody java.util.Map<String,String> body) {
        String roomId = body.getOrDefault("roomId", "");
        if (roomId.isEmpty()) return ResponseEntity.badRequest().body("roomId required");
        // for now, no persistent state is changed; this endpoint notifies that room should start
        return ResponseEntity.ok().build();
    }
}
