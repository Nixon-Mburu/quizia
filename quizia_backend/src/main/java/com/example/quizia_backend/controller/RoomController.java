package com.example.quizia_backend.controller;

import com.example.quizia_backend.model.Room;
import com.example.quizia_backend.model.Question;
import com.example.quizia_backend.repository.AnswerRepository;
import com.example.quizia_backend.repository.RoomRepository;
import com.example.quizia_backend.repository.RoomMemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final com.example.quizia_backend.repository.QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;


    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();


    private final Map<String, List<com.example.quizia_backend.model.Question>> roomQuestions = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Set<String>>> roomAnswers = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> syncEmitters = new ConcurrentHashMap<>();
    
    // Track question timers for auto-advance
    private final Map<String, Map<Integer, Long>> questionStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> currentQuestionIndex = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(5);
    private final static int QUESTION_TIMER_SECONDS = 10;

    public RoomController(RoomRepository roomRepository, RoomMemberRepository memberRepository,
                         com.example.quizia_backend.repository.QuestionRepository questionRepository,
                         AnswerRepository answerRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @GetMapping
    public List<Room> listRooms() {
        List<Room> rooms = roomRepository.findAll();

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

            }
            System.out.println("[RoomController] Listing room: " + r.getRoomName() + " | Creator: " + r.getCreatedByUsername() + " | Members: " + r.getMemberNames());
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
            System.out.println("[RoomController] Registering room: " + room.getRoomName() + " | Creator: " + room.getCreatedByUsername() + " | RoomId: " + room.getRoomId());
            roomRepository.addRoom(room);
            return ResponseEntity.status(201).build();
        } catch (Exception ex) {
            System.out.println("[RoomController] Error registering room: " + ex.getMessage());
            return ResponseEntity.status(500).body("could not register room: " + ex.getMessage());
        }
    }

    @PostMapping("/start")
    public ResponseEntity<?> startRoom(@RequestBody java.util.Map<String,String> body) {
        String roomId = body.getOrDefault("roomId", "");
        String username = body.getOrDefault("username", "");
        if (roomId.isEmpty()) return ResponseEntity.badRequest().body("roomId required");

        // Verify that the requester is the room creator
        List<Room> rooms = roomRepository.findAll();
        Room room = rooms.stream().filter(r -> r.getRoomId().equals(roomId)).findFirst().orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        String creator = room.getCreatedByUsername() == null ? "" : room.getCreatedByUsername();
        if (username == null || username.isEmpty() || !creator.equals(username)) {
            System.out.println("[RoomController] Unauthorized start attempt by '" + username + "' (creator='" + creator + "') for room: " + roomId);
            return ResponseEntity.status(403).body("only the room creator can start the quiz");
        }

        System.out.println("[RoomController] Broadcasting GAME_STARTED to room: " + roomId + " by " + username);

        // Generate questions for the room if not already cached
        if (!roomQuestions.containsKey(roomId)) {
            String topic = room.getTopics();
            if (topic == null || topic.isEmpty()) {
                topic = "General Knowledge";
            }
            if (topic.contains(",")) {
                topic = topic.split(",")[0].trim();
            }
            
            List<com.example.quizia_backend.model.Question> allQuestions = questionRepository.findByTopic(topic, 50);
            java.util.Collections.shuffle(allQuestions);
            List<com.example.quizia_backend.model.Question> selected = allQuestions.stream()
                .limit(6)
                .collect(java.util.stream.Collectors.toList());
            
            roomQuestions.put(roomId, selected);
            roomAnswers.putIfAbsent(roomId, new ConcurrentHashMap<>());
            
            System.out.println("[RoomController] Generated " + selected.size() + " questions for room: " + roomId);
        }

        // Provide a small sync delay so clients can align timers
        long startTime = System.currentTimeMillis() + 2000L; // start in 2 seconds

        // Build JSON with questions included manually (to avoid Jackson dependency issues)
        try {
            List<com.example.quizia_backend.model.Question> questions = roomQuestions.get(roomId);
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"action\":\"GAME_STARTED\",\"startTime\":").append(startTime).append(",\"questions\":[");
            
            for (int i = 0; i < questions.size(); i++) {
                if (i > 0) jsonBuilder.append(",");
                com.example.quizia_backend.model.Question q = questions.get(i);
                jsonBuilder.append("{")
                    .append("\"id\":").append(q.getId()).append(",")
                    .append("\"question\":\"").append(escapeJson(q.getQuestion())).append("\",")
                    .append("\"optionA\":\"").append(escapeJson(q.getOptionA())).append("\",")
                    .append("\"optionB\":\"").append(escapeJson(q.getOptionB())).append("\",")
                    .append("\"optionC\":\"").append(escapeJson(q.getOptionC())).append("\",")
                    .append("\"optionD\":\"").append(escapeJson(q.getOptionD())).append("\",")
                    .append("\"correctOption\":\"").append(escapeJson(q.getCorrectOption())).append("\",")
                    .append("\"topic\":\"").append(escapeJson(q.getTopic())).append("\"")
                    .append("}");
            }
            jsonBuilder.append("]}");
            
            String json = jsonBuilder.toString();
            
            List<SseEmitter> list = emitters.get(roomId);
            if (list != null) {
                for (SseEmitter e : list) {
                    try {
                        SseEmitter.SseEventBuilder ev = SseEmitter.event().name("start").data(json);
                        e.send(ev);
                    } catch (Exception ex) {
                        System.err.println("[RoomController] Error sending SSE: " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[RoomController] Error building start payload: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.status(500).body("failed to build start payload");
        }
        
        return ResponseEntity.ok().build();
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // Compatibility path: some frontends request /sse
    @GetMapping("/{roomId}/sse")
    public SseEmitter subscribeSse(@PathVariable String roomId) {
        return subscribe(roomId);
    }

    @GetMapping("/{roomId}/events")
    public SseEmitter subscribe(@PathVariable String roomId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> emitters.getOrDefault(roomId, List.of()).remove(emitter));
        emitter.onTimeout(() -> emitters.getOrDefault(roomId, List.of()).remove(emitter));
        try {

            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (Exception ex) {

        }
        return emitter;
    }


    @GetMapping("/events")
    public SseEmitter subscribeByParam(@RequestParam("roomId") String roomId) {
        return subscribe(roomId);
    }


    @GetMapping("/{roomId}/questions")
    public ResponseEntity<?> getRoomQuestions(@PathVariable String roomId) {
        try {
            // Return cached questions if they exist (generated when quiz started)
            if (!roomQuestions.containsKey(roomId)) {
                // Fallback: generate questions if somehow requested before start
                List<Room> rooms = roomRepository.findAll();
                Room room = rooms.stream()
                    .filter(r -> r.getRoomId().equals(roomId))
                    .findFirst()
                    .orElse(null);

                if (room == null) {
                    return ResponseEntity.notFound().build();
                }

                String topic = room.getTopics();
                if (topic == null || topic.isEmpty()) {
                    topic = "General Knowledge";
                }

                if (topic.contains(",")) {
                    topic = topic.split(",")[0].trim();
                }

                List<com.example.quizia_backend.model.Question> allQuestions = questionRepository.findByTopic(topic, 50);

                // Shuffle ONCE per room, then cache
                java.util.Collections.shuffle(allQuestions);
                List<com.example.quizia_backend.model.Question> selected = allQuestions.stream()
                    .limit(6)
                    .collect(java.util.stream.Collectors.toList());

                roomQuestions.put(roomId, selected);
                System.out.println("[RoomController] Fallback: Generated " + selected.size() + " questions for room: " + roomId);


                roomAnswers.putIfAbsent(roomId, new ConcurrentHashMap<>());
                
                // Schedule auto-advance for the first question
                String timerKey = roomId + "_Q0";
                if (!currentQuestionIndex.containsKey(timerKey)) {
                    currentQuestionIndex.put(timerKey, 1);
                    timerExecutor.schedule(() -> {
                        broadcastToSync(roomId, "NEXT_QUESTION:0");
                        currentQuestionIndex.remove(timerKey);
                    }, QUESTION_TIMER_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                }
            }

            return ResponseEntity.ok(roomQuestions.get(roomId));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching questions: " + ex.getMessage());
        }
    }


    @PostMapping("/answer")
    public ResponseEntity<?> submitAnswer(@RequestBody Map<String, Object> payload) {
        try {
            String roomId = (String) payload.get("roomId");
            String username = (String) payload.get("username");
            Integer questionIndex = (Integer) payload.get("questionIndex");
            String answer = (String) payload.get("answer");
            Long timeMs = ((Number) payload.get("timeMs")).longValue();

            if (roomId == null || username == null || questionIndex == null || answer == null || timeMs == null) {
                return ResponseEntity.badRequest().body("roomId, username, questionIndex, answer, and timeMs required");
            }

            // Store the answer timing data
            com.example.quizia_backend.model.Result.Answer answerRecord = new com.example.quizia_backend.model.Result.Answer();
            answerRecord.setRoomId(roomId);
            answerRecord.setUsername(username);
            answerRecord.setQuestionIndex(questionIndex);
            answerRecord.setSelectedAnswer(answer);
            answerRecord.setTimeMs(timeMs);

            // Check if answer is correct by comparing with cached questions
            List<com.example.quizia_backend.model.Question> questions = roomQuestions.get(roomId);
            if (questions != null && questionIndex < questions.size()) {
                String correctAnswer = questions.get(questionIndex).getCorrectOption();
                boolean isCorrect = correctAnswer != null && correctAnswer.equalsIgnoreCase(answer);
                answerRecord.setCorrect(isCorrect);
            } else {
                answerRecord.setCorrect(false);
            }

            answerRepository.save(answerRecord);

            roomAnswers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                       .computeIfAbsent(questionIndex, k -> java.util.Collections.synchronizedSet(new java.util.HashSet<>()))
                       .add(username);


            int memberCount = memberRepository.countMembers(roomId);
            int answeredCount = roomAnswers.get(roomId).get(questionIndex).size();

            System.out.println("[Room " + roomId + "] Question " + questionIndex + ": " + answeredCount + "/" + memberCount + " answered");

            // Broadcast NEXT_QUESTION if all members have answered
            if (answeredCount >= memberCount && memberCount > 0) {
                broadcastToSync(roomId, "NEXT_QUESTION:" + questionIndex);
            }
            
            // Schedule auto-advance after 10 seconds if not already scheduled
            String timerKey = roomId + "_Q" + questionIndex;
            if (!currentQuestionIndex.containsKey(timerKey)) {
                currentQuestionIndex.put(timerKey, 1); // Mark as scheduled
                timerExecutor.schedule(() -> {
                    // Auto-advance after 10 seconds
                    broadcastToSync(roomId, "NEXT_QUESTION:" + questionIndex);
                    currentQuestionIndex.remove(timerKey);
                }, QUESTION_TIMER_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            }

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("Error submitting answer: " + ex.getMessage());
        }
    }


    @GetMapping("/{roomId}/sync")
    public SseEmitter subscribeSync(@PathVariable String roomId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        syncEmitters.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> syncEmitters.getOrDefault(roomId, List.of()).remove(emitter));
        emitter.onTimeout(() -> syncEmitters.getOrDefault(roomId, List.of()).remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("sync"));
        } catch (Exception ex) {

        }

        return emitter;
    }

    private void broadcastToSync(String roomId, String message) {
        List<SseEmitter> emitterList = syncEmitters.get(roomId);
        if (emitterList != null) {
            for (SseEmitter emitter : emitterList) {
                try {
                    emitter.send(SseEmitter.event().data(message));
                } catch (Exception ex) {

                }
            }
        }
    }
}