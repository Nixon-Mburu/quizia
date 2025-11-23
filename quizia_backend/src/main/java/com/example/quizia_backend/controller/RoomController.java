package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Room;
import com.example.quizia_backend.repository.RoomRepository;
import com.example.quizia_backend.repository.RoomMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    // SSE emitters per roomId
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

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
        // notify subscribers (SSE) that the room should start
        List<SseEmitter> list = emitters.get(roomId);
        if (list != null) {
            for (SseEmitter e : list) {
                try {
                    SseEmitter.SseEventBuilder ev = SseEmitter.event().name("start").data("started");
                    e.send(ev);
                } catch (Exception ex) {
                    // ignore; emitter may be completed
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}/events")
    public SseEmitter subscribe(@PathVariable String roomId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minutes
        emitters.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> emitters.getOrDefault(roomId, List.of()).remove(emitter));
        emitter.onTimeout(() -> emitters.getOrDefault(roomId, List.of()).remove(emitter));
        try {
            // send a comment or ping
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (Exception ex) {
            // ignore
        }
        return emitter;
    }

    // Alternative subscribe endpoint that accepts roomId as query parameter.
    @GetMapping("/events")
    public SseEmitter subscribeByParam(@RequestParam("roomId") String roomId) {
        return subscribe(roomId);
    }
}
