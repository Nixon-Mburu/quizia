package com.example.quizia.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.application.Platform;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import com.example.quizia.frontend.config.ServerConfig;

public class JoinRoomController {

    @FXML
    private VBox roomCardsContainer;

    @FXML
    private Button backButton;

    @FXML
    private Label roomCountLabel;

    @FXML
    private AnchorPane root;
    
    @FXML
    private VBox currentMembersSection;
    
    @FXML
    private FlowPane currentMembersChips;

    private List<Room> rooms = new java.util.ArrayList<>();
    private String currentRoomId = null;
    private boolean isWaitingInRoom = false;
    private Thread sseThread = null;
    private List<String> currentMembers = new java.util.ArrayList<>();

    @FXML
    private void initialize() {

        fetchRoomsFromBackend();
        
        // Poll for room updates every 2 seconds for real-time member list updates
        Thread pollingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000);
                    fetchRoomsFromBackend();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();


        if (backButton != null) {
            backButton.setOnAction(e -> {
                cleanup();
                goBackToAuth();
            });
        }


        root.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin != null) {
                        fetchRoomsFromBackend();
                    }
                });
            }
        });
    }

    private void cleanup() {
        isWaitingInRoom = false;
        if (sseThread != null && sseThread.isAlive()) {
            sseThread.interrupt();
        }
    }

    private void goBackToAuth() {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/fxml/auth.fxml"));
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(page));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renderRoomCards() {
        roomCardsContainer.getChildren().clear();

        if (rooms.isEmpty()) {
            Label emptyLabel = new Label("No rooms available. Create one to get started!");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #5C4A3A; -fx-padding: 40 0 0 0;");
            roomCardsContainer.getChildren().add(emptyLabel);
            if (roomCountLabel != null) {
                roomCountLabel.setText("No rooms");
            }
            return;
        }

        if (roomCountLabel != null) {
            roomCountLabel.setText(rooms.size() + " room(s) available");
        }

        String currentUser = "";
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(getClass());
            currentUser = prefs.get("quizia.username", "").trim();
            System.out.println("[DEBUG] renderRoomCards - currentUser from prefs: '" + currentUser + "'");
        } catch (Exception ex) {
            System.out.println("[ERROR] Failed to get username from prefs: " + ex.getMessage());
        }

        for (Room room : rooms) {
            // Update the current members display if we're in this room
            if (isWaitingInRoom && currentRoomId != null && currentRoomId.equals(room.getRoomId())) {
                updateCurrentMembersDisplay(room);
            }
            // Debug: Check if this is creator's room
            String createdBy = room.getCreatedByUsername() != null ? room.getCreatedByUsername() : "null";
            System.out.println("[DEBUG] Room: " + room.getRoomName() + " | Created by: '" + createdBy + "' | Current user: '" + currentUser + "' | IsCreator: " + (room.getCreatedByUsername() != null && !room.getCreatedByUsername().isEmpty() && room.getCreatedByUsername().equals(currentUser)));
            VBox card = createRoomCard(room, currentUser);
            roomCardsContainer.getChildren().add(card);
        }
    }

    private VBox createRoomCard(Room room, String currentUser) {
        VBox card = new VBox(12);
        card.getStyleClass().add("room-card");
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20px; -fx-border-radius: 20px; " +
                "-fx-border-color: #2C1810; -fx-border-width: 2px; -fx-padding: 24; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4);");

        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(room.getRoomName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2C1810;");

        Label idBadge = new Label("ID: " + room.getRoomId());
        idBadge.setStyle("-fx-font-size: 12px; -fx-text-fill: #667eea; -fx-background-color: #EEF2FF; " +
                "-fx-padding: 4 12; -fx-background-radius: 12px;");

        header.getChildren().addAll(nameLabel, idBadge);

        // Topics
        HBox topicsBox = new HBox(8);
        topicsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label topicsIcon = new Label("📚");
        topicsIcon.setStyle("-fx-font-size: 16px;");
        Label topicsLabel = new Label("Topics: " + (room.getTopics() != null ? room.getTopics() : "N/A"));
        topicsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #5C4A3A;");
        topicsBox.getChildren().addAll(topicsIcon, topicsLabel);

        // Members
        HBox membersBox = new HBox(8);
        membersBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label membersIcon = new Label("👥");
        membersIcon.setStyle("-fx-font-size: 16px;");
        Label membersCount = new Label(room.getMemberNumber() + " player(s)");
        membersCount.setStyle("-fx-font-size: 14px; -fx-text-fill: #5C4A3A; -fx-font-weight: 600;");
        membersBox.getChildren().addAll(membersIcon, membersCount);

        FlowPane membersChips = new FlowPane(8, 8);
        if (room.getMemberNames() != null && !room.getMemberNames().trim().isEmpty()) {
            String[] members = room.getMemberNames().split(",");
            for (String member : members) {
                Label chip = new Label(member.trim());
                chip.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4F46E5; " +
                        "-fx-padding: 6 12; -fx-background-radius: 12px; -fx-font-size: 12px; -fx-font-weight: 600;");
                membersChips.getChildren().add(chip);
            }
        }

        // Creator info
        HBox creatorBox = new HBox(8);
        creatorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label creatorIcon = new Label("⭐");
        creatorIcon.setStyle("-fx-font-size: 14px;");
        Label creatorLabel = new Label("Created by: " + (room.getCreatedByUsername() != null ? room.getCreatedByUsername() : "Unknown"));
        creatorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8B7355; -fx-font-style: italic;");
        creatorBox.getChildren().addAll(creatorIcon, creatorLabel);

        // Actions
        HBox actionsBox = new HBox(12);
        actionsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        actionsBox.setStyle("-fx-padding: 12 0 0 0;");

        Button joinBtn = new Button("Join Room");
        joinBtn.getStyleClass().add("btn-primary");
        joinBtn.setStyle("-fx-background-color: linear-gradient(#667eea, #5568d3); " +
                "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 14px; " +
                "-fx-padding: 10 24; -fx-background-radius: 10px; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 10, 0, 0, 3);");
        joinBtn.setOnAction(e -> handleJoinRoom(room));
        actionsBox.getChildren().add(joinBtn);

        boolean isCreator = room.getCreatedByUsername() != null &&
                !room.getCreatedByUsername().isEmpty() &&
                room.getCreatedByUsername().equals(currentUser);

        System.out.println("[DEBUG] createRoomCard - Room: " + room.getRoomName() + " | isCreator: " + isCreator + 
                " | room creator: '" + (room.getCreatedByUsername() != null ? room.getCreatedByUsername() : "null") + 
                "' | currentUser: '" + currentUser + "'");

        if (isCreator) {
            System.out.println("[DEBUG] Adding Start Quiz button for creator: " + currentUser);
            Button startBtn = new Button("Start Quiz ▶");
            startBtn.getStyleClass().add("btn-success");
            startBtn.setStyle("-fx-background-color: linear-gradient(#10B981, #059669); " +
                    "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 14px; " +
                    "-fx-padding: 10 24; -fx-background-radius: 10px; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.3), 10, 0, 0, 3);");
            startBtn.setOnAction(e -> handleStartRoom(room));
            actionsBox.getChildren().add(startBtn);
        } else {
            System.out.println("[DEBUG] NOT a creator - button NOT added");
        }

        card.getChildren().addAll(header, topicsBox, membersBox, membersChips, creatorBox, actionsBox);
        return card;
    }

    private void fetchRoomsFromBackend() {

        new Thread(() -> {
            try {
                String url = ServerConfig.getBackendUrl() + "/api/rooms";
                System.out.println("[JoinRoomController] Fetching rooms from: " + url);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[JoinRoomController] Response status: " + resp.statusCode());
                if (resp.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<java.util.Map<String,Object>> list = mapper.readValue(resp.body(), new TypeReference<List<java.util.Map<String,Object>>>(){});
                    List<Room> fetchedRooms = new java.util.ArrayList<>();
                    for (java.util.Map<String,Object> m : list) {
                        String rid = java.util.Objects.toString(m.getOrDefault("roomId", m.getOrDefault("room_id", "")), "");
                        String rname = java.util.Objects.toString(m.getOrDefault("roomName", m.getOrDefault("room_name", "")), "");
                        int mc = 0;
                        try {
                            Object mcObj = m.getOrDefault("memberCount", m.getOrDefault("member_count", 0));
                            if (mcObj != null) mc = Integer.parseInt(java.util.Objects.toString(mcObj, "0"));
                        } catch (Exception ex) {
                            mc = 0;
                        }
                        String mn = java.util.Objects.toString(m.getOrDefault("memberNames", m.getOrDefault("member_names", "")), "");
                        String topics = java.util.Objects.toString(m.getOrDefault("topics", ""), "");
                        String createdBy = java.util.Objects.toString(m.getOrDefault("createdByUsername", m.getOrDefault("created_by_username", "")), "");
                        fetchedRooms.add(new Room(rid, rname, mc, mn, topics, createdBy));
                    }
                    System.out.println("[JoinRoomController] Fetched " + fetchedRooms.size() + " rooms");
                    Platform.runLater(() -> {
                        rooms = fetchedRooms;
                        renderRoomCards();
                    });
                } else {
                    System.err.println("[JoinRoomController] Failed to fetch rooms: " + resp.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[JoinRoomController] Exception fetching rooms: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void handleJoinRoom(Room room) {

        new Thread(() -> {
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(getClass());
                String uname = prefs.get("quizia.username", "anonymous");


                ObjectMapper mapper = new ObjectMapper();
                java.util.Map<String,String> payload = new java.util.HashMap<>();
                payload.put("roomId", room.getRoomId());
                payload.put("username", uname == null || uname.isEmpty() ? "anonymous" : uname);
                String body = mapper.writeValueAsString(payload);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.getBackendUrl() + "/api/rooms/join"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {

                    Platform.runLater(() -> {
                        showJoinNotification(payload.get("username") + " joined");
                        // Show the members section
                        if (currentMembersSection != null) {
                            currentMembersSection.setVisible(true);
                            currentMembersSection.setManaged(true);
                        }
                        updateCurrentMembersDisplay(room);
                    });


                    currentRoomId = room.getRoomId();
                    isWaitingInRoom = true;
                    startListeningForGameStart(room, payload.get("username"));
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Join Failed");
                        alert.setHeaderText(null);
                        alert.setContentText("Could not join room: " + resp.statusCode() + " " + resp.body());
                        alert.showAndWait();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Join Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Error joining room: " + ex.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    private void updateCurrentMembersDisplay(Room room) {
        if (currentMembersChips == null) return;
        
        currentMembers.clear();
        if (room.getMemberNames() != null && !room.getMemberNames().trim().isEmpty()) {
            String[] members = room.getMemberNames().split(",");
            for (String member : members) {
                currentMembers.add(member.trim());
            }
        }
        
        Platform.runLater(() -> {
            currentMembersChips.getChildren().clear();
            for (String memberName : currentMembers) {
                Label chip = new Label(memberName);
                chip.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; " +
                        "-fx-padding: 8 16; -fx-background-radius: 16px; -fx-font-size: 13px; -fx-font-weight: 600;");
                currentMembersChips.getChildren().add(chip);
            }
        });
    }

    private void handleStartRoom(Room room) {

        new Thread(() -> {
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(getClass());
                String uname = prefs.get("quizia.username", "anonymous");

                java.util.Map<String,String> payload = new java.util.HashMap<>();
                payload.put("roomId", room.getRoomId());
                payload.put("username", uname == null || uname.isEmpty() ? "anonymous" : uname);
                ObjectMapper mapper = new ObjectMapper();
                String body = mapper.writeValueAsString(payload);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.getBackendUrl() + "/api/rooms/start"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/questions.fxml"));
                            Parent page = loader.load();
                            com.example.quizia.frontend.controller.QuestionsController qc = loader.getController();
                            qc.setRoomInfo(room.getRoomId(), room.getRoomName());
                            qc.setUsername(uname);
                            String topic = null;
                            if (room.getTopics() != null && !room.getTopics().isEmpty()) {
                                String[] parts = room.getTopics().split(",");
                                if (parts.length > 0) topic = parts[0].trim();
                            }
                            if (topic == null || topic.isEmpty()) topic = "General Knowledge";
                            qc.setTopic(topic);
                            Stage stage = (Stage) root.getScene().getWindow();
                            stage.setScene(new Scene(page));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Start Failed");
                        alert.setHeaderText(null);
                        alert.setContentText("Could not start room: " + resp.statusCode());
                        alert.showAndWait();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void startListeningForGameStart(Room room, String username) {

        sseThread = new Thread(() -> {
            try {
                System.out.println("[JoinRoomController] Starting SSE listener for room: " + room.getRoomId());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.getBackendUrl() + "/api/rooms/" + room.getRoomId() + "/sse"))
                        .GET()
                        .build();

                client.sendAsync(req, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(resp -> {
                        resp.body().forEach(line -> {
                            if (!isWaitingInRoom) return;
                            
                            System.out.println("[JoinRoomController] SSE Event: " + line);

                            if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();
                                System.out.println("[JoinRoomController] SSE Data: " + data);


                                if (data.contains("USER_JOINED")) {

                                    try {
                                        String joinedUser = data.substring(data.indexOf(":") + 1).trim();
                                        if (!joinedUser.equals(username)) {
                                            Platform.runLater(() -> showJoinNotification(joinedUser + " joined"));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else if (data.contains("GAME_STARTED") || data.trim().startsWith("{")) {
                                    System.out.println("[JoinRoomController] GAME_STARTED received (raw): " + data);
                                    // try to parse structured JSON payload if present
                                    long startTime = 0L;
                                    java.util.List<com.example.quizia.frontend.model.Question> questions = null;
                                    try {
                                        if (data.trim().startsWith("{")) {
                                            ObjectMapper mapper2 = new ObjectMapper();
                                            java.util.Map<String,Object> m = mapper2.readValue(data, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>>(){});
                                            if (m.containsKey("startTime")) {
                                                Object st = m.get("startTime");
                                                if (st instanceof Number) startTime = ((Number)st).longValue();
                                                else startTime = Long.parseLong(java.util.Objects.toString(st, "0"));
                                            }
                                            if (m.containsKey("questions")) {
                                                // Parse questions array from start payload
                                                questions = mapper2.convertValue(m.get("questions"), 
                                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.example.quizia.frontend.model.Question>>(){});
                                                System.out.println("[JoinRoomController] Received " + (questions == null ? 0 : questions.size()) + " questions in start event");
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.err.println("[JoinRoomController] Failed to parse start payload: " + e.getMessage());
                                        e.printStackTrace();
                                    }

                                    isWaitingInRoom = false;
                                    final long finalStartTime = startTime;
                                    final java.util.List<com.example.quizia.frontend.model.Question> finalQuestions = questions;
                                    Platform.runLater(() -> {
                                        try {
                                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/questions.fxml"));
                                            Parent page = loader.load();
                                            com.example.quizia.frontend.controller.QuestionsController qc = loader.getController();
                                            qc.setRoomInfo(room.getRoomId(), room.getRoomName());
                                            qc.setUsername(username);
                                            if (finalStartTime > 0) qc.setQuizStartTime(finalStartTime);
                                            
                                            // Pass questions directly if received in start event
                                            if (finalQuestions != null && !finalQuestions.isEmpty()) {
                                                qc.setQuestions(finalQuestions);
                                                System.out.println("[JoinRoomController] Passed " + finalQuestions.size() + " questions to QuestionsController");
                                            } else {
                                                // Fallback: fetch from server
                                                String topic = null;
                                                if (room.getTopics() != null && !room.getTopics().isEmpty()) {
                                                    String[] parts = room.getTopics().split(",");
                                                    if (parts.length > 0) topic = parts[0].trim();
                                                }
                                                if (topic == null || topic.isEmpty()) topic = "General Knowledge";
                                                qc.setTopic(topic);
                                            }
                                            Stage stage = (Stage) root.getScene().getWindow();
                                            stage.setScene(new Scene(page));
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    });
                                }
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        sseThread.setDaemon(true);
        sseThread.start();
    }

    private void showJoinNotification(String message) {

        VBox toast = new VBox();
        toast.setStyle("-fx-background-color: linear-gradient(to right, #10B981 0%, #059669 100%); " +
                      "-fx-background-radius: 15px; -fx-padding: 16 24; " +
                      "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.4), 20, 0, 0, 5);");

        Label toastLabel = new Label(message);
        toastLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700;");
        toast.getChildren().add(toastLabel);


        toast.setLayoutX((root.getWidth() - 300) / 2);
        toast.setLayoutY(20);
        toast.setOpacity(0);

        root.getChildren().add(toast);


        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(400), toast);
        slideIn.setFromY(-50);
        slideIn.setToY(0);

        javafx.animation.ParallelTransition showAnim = new javafx.animation.ParallelTransition(fadeIn, slideIn);
        showAnim.play();


        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        pause.setOnFinished(e -> {
            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(javafx.util.Duration.millis(400), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> root.getChildren().remove(toast));
            fadeOut.play();
        });
        pause.play();
    }


    public static class Room {
        private String roomId;
        private String roomName;
        private int memberNumber;
        private String memberNames;
        private String topics;
        private String createdByUsername;

            public Room(String roomId, String roomName, int memberNumber, String memberNames) {
                this(roomId, roomName, memberNumber, memberNames, "", "");
            }

            public Room(String roomId, String roomName, int memberNumber, String memberNames, String topics) {
                this(roomId, roomName, memberNumber, memberNames, topics, "");
            }

            public Room(String roomId, String roomName, int memberNumber, String memberNames, String topics, String createdByUsername) {
                this.roomId = roomId;
                this.roomName = roomName;
                this.memberNumber = memberNumber;
                this.memberNames = memberNames;
                this.topics = topics == null ? "" : topics;
                this.createdByUsername = createdByUsername == null ? "" : createdByUsername;
            }
        public String getRoomId() { return roomId; }
        public String getRoomName() { return roomName; }
        public int getMemberNumber() { return memberNumber; }
        public String getMemberNames() { return memberNames; }
        public String getTopics() { return topics; }
        public String getCreatedByUsername() { return createdByUsername; }
    }
}