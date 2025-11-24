package com.example.quizia_backend.controller;

import com.example.quizia_backend.repository.RoomMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomMemberController {
    private final RoomMemberRepository repo;

    public RoomMemberController(RoomMemberRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody java.util.Map<String,String> body) {
        String roomId = body.getOrDefault("roomId", "");
        String username = body.getOrDefault("username", "");
        if (roomId.isEmpty() || username.isEmpty()) {
            return ResponseEntity.badRequest().body("roomId and username required");
        }
        try {
            repo.addMember(roomId, username);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("error: " + ex.getMessage());
        }
    }
}