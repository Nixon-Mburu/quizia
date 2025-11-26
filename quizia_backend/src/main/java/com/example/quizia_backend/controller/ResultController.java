package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Result;
import com.example.quizia_backend.repository.ResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResultController {
    private final ResultRepository repo;

    public ResultController(ResultRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/results/submit")
    public ResponseEntity<String> submitResult(@RequestBody Result r) {
        try {
            System.out.println("[ResultController] Submitting result for room: " + r.getRoomId() + " | user: " + r.getUsername() + " | correct: " + r.getCorrect());
            repo.save(r);
            return ResponseEntity.ok("saved");
        } catch (Exception ex) {
            System.err.println("[ResultController] Error saving result: " + ex.getMessage());
            return ResponseEntity.status(500).body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/results")
    public ResponseEntity<?> getResults(@RequestParam String room) {
        try {
            System.out.println("[ResultController] Fetching results for room: " + room);
            List<Result> results = repo.findByRoomId(room);
            System.out.println("[ResultController] Found " + results.size() + " results");
            return ResponseEntity.ok(results);
        } catch (Exception ex) {
            System.err.println("[ResultController] Error fetching results: " + ex.getMessage());
            return ResponseEntity.status(500).body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/leaderboard")
    public List<Result> leaderboard(@RequestParam String roomId) {
        return repo.findByRoomId(roomId);
    }
}