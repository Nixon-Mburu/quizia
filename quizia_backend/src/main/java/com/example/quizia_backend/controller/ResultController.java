package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Result;
import com.example.quizia_backend.repository.ResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ResultController {
    private final ResultRepository repo;

    public ResultController(ResultRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/results")
    public ResponseEntity<String> submitResult(@RequestBody Result r) {
        try {
            repo.save(r);
            return ResponseEntity.ok("saved");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/leaderboard")
    public List<Result> leaderboard(@RequestParam String roomId) {
        return repo.findByRoomId(roomId);
    }
}
