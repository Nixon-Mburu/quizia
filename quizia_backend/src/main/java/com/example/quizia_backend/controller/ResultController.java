package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Result;
import com.example.quizia_backend.repository.AnswerRepository;
import com.example.quizia_backend.repository.ResultRepository;
import com.example.quizia_backend.repository.RoomMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ResultController {
    private final ResultRepository repo;
    private final AnswerRepository answerRepo;
    private final RoomMemberRepository memberRepo;

    public ResultController(ResultRepository repo, AnswerRepository answerRepo, RoomMemberRepository memberRepo) {
        this.repo = repo;
        this.answerRepo = answerRepo;
        this.memberRepo = memberRepo;
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

            // Get all room members
            List<Map<String, Object>> members = memberRepo.listMembers(room);
            System.out.println("[ResultController] Found " + members.size() + " room members");

            // Get submitted results
            List<Result> submittedResults = repo.findByRoomId(room);
            System.out.println("[ResultController] Found " + submittedResults.size() + " submitted results");

            // Create a map of username -> result for quick lookup
            Map<String, Result> resultMap = submittedResults.stream()
                .collect(Collectors.toMap(Result::getUsername, r -> r));

            // Merge members with results - ensure all members appear
            List<Result> allResults = members.stream()
                .map(member -> {
                    String username = (String) member.get("username");
                    Result existingResult = resultMap.get(username);

                    if (existingResult != null) {
                        return existingResult;
                    } else {
                        // Create a placeholder result for members who haven't submitted yet
                        Result placeholder = new Result();
                        placeholder.setId(0); // Placeholder ID
                        placeholder.setRoomId(room);
                        placeholder.setUsername(username);
                        placeholder.setCorrect(0); // No correct answers yet
                        placeholder.setTotalTimeMs(0); // No time yet
                        return placeholder;
                    }
                })
                .collect(Collectors.toList());

            // Sort by correct (descending), then by time (ascending)
            allResults.sort((a, b) -> {
                if (b.getCorrect() != a.getCorrect()) {
                    return Integer.compare(b.getCorrect(), a.getCorrect());
                }
                return Long.compare(a.getTotalTimeMs(), b.getTotalTimeMs());
            });

            System.out.println("[ResultController] Returning " + allResults.size() + " total results (including placeholders)");
            return ResponseEntity.ok(allResults);
        } catch (Exception ex) {
            System.err.println("[ResultController] Error fetching results: " + ex.getMessage());
            return ResponseEntity.status(500).body("error: " + ex.getMessage());
        }
    }

    @GetMapping("/answers")
    public ResponseEntity<?> getAnswers(@RequestParam String room, @RequestParam String username) {
        try {
            System.out.println("[ResultController] Fetching answers for room: " + room + ", user: " + username);
            List<com.example.quizia_backend.model.Result.Answer> answers = answerRepo.findByRoomIdAndUsername(room, username);
            System.out.println("[ResultController] Found " + answers.size() + " answers");
            return ResponseEntity.ok(answers);
        } catch (Exception ex) {
            System.err.println("[ResultController] Error fetching answers: " + ex.getMessage());
            return ResponseEntity.status(500).body("error: " + ex.getMessage());
        }
    }
}