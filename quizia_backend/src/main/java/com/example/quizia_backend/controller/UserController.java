package com.example.quizia_backend.controller;

import com.example.quizia_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserRepository repo;

    public UserController(UserRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody java.util.Map<String,String> body) {
        String username = body.getOrDefault("username", "");
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("username required");
        }
        repo.addUser(username.trim());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<java.util.Map<String,Object>> listUsers() {
        return repo.findAll();
    }
}