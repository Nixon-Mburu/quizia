package com.example.quizia.frontend.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.quizia.frontend.config.ServerConfig;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

public class LeaderboardController {
    @FXML private Button backBtn;
    @FXML private AnchorPane root;
    @FXML private Label firstName;
    @FXML private Label firstScore;
    @FXML private Label firstTime;
    @FXML private Label secondName;
    @FXML private Label secondScore;
    @FXML private Label secondTime;
    @FXML private Label thirdName;
    @FXML private Label thirdScore;
    @FXML private Label thirdTime;
    @FXML private VBox otherPlayersList;
    @FXML private VBox bar1Container;
    @FXML private VBox bar2Container;
    @FXML private VBox bar3Container;

    private String roomId;

    public void setRoomId(String roomId) {
        this.roomId = roomId;
        fetchLeaderboard();
    }

    @FXML
    private void initialize() {
        backBtn.setOnAction(e -> {
            try {
                Parent page = FXMLLoader.load(getClass().getResource("/fxml/join_room.fxml"));
                Stage stage = (Stage) root.getScene().getWindow();
                stage.setScene(new Scene(page));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void fetchLeaderboard() {
        new Thread(() -> {
            try {
                String url = ServerConfig.getBackendUrl() + "/api/results?room=" + roomId;
                System.out.println("[LeaderboardController] Fetching results from: " + url);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                
                if (resp.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<java.util.Map<String,Object>> list = mapper.readValue(resp.body(), new TypeReference<List<java.util.Map<String,Object>>>(){});
                    
                    System.out.println("[LeaderboardController] Fetched " + list.size() + " results");
                    
                    // Create result rows with detailed timing
                    List<ResultRow> rows = list.stream().map(m -> {
                        String username = java.util.Objects.toString(m.getOrDefault("username", ""));
                        int correct = Integer.parseInt(java.util.Objects.toString(m.getOrDefault("correct", "0")));
                        long totalTime = Long.parseLong(java.util.Objects.toString(m.getOrDefault("total_time_ms", m.getOrDefault("totalTimeMs", "0"))));
                        
                        // Check if this is a placeholder result (user hasn't completed quiz)
                        boolean hasCompleted = totalTime > 0 || correct > 0;
                        
                        // Fetch detailed per-question timing only if user has completed
                        List<AnswerDetail> answerDetails = hasCompleted ? fetchAnswerDetails(username) : new java.util.ArrayList<>();
                        
                        return new ResultRow(username, correct, totalTime, answerDetails, hasCompleted);
                    }).collect(Collectors.toList());
                    
                    // Sort by correct (descending), then by time (ascending)
                    rows.sort((a, b) -> {
                        if (b.correct != a.correct) {
                            return Integer.compare(b.correct, a.correct);
                        }
                        return Long.compare(a.totalTimeMs, b.totalTimeMs);
                    });
                    
                    System.out.println("[LeaderboardController] Sorted results: " + rows.size());
                    
                    Platform.runLater(() -> {
                        // Populate podium
                        if (rows.size() > 0) {
                            firstName.setText(rows.get(0).username);
                            firstScore.setText(rows.get(0).hasCompleted ? rows.get(0).correct + " pts" : "Not completed");
                            firstTime.setText(rows.get(0).hasCompleted ? formatTime(rows.get(0).totalTimeMs) : "--");
                        }
                        if (rows.size() > 1) {
                            secondName.setText(rows.get(1).username);
                            secondScore.setText(rows.get(1).hasCompleted ? rows.get(1).correct + " pts" : "Not completed");
                            secondTime.setText(rows.get(1).hasCompleted ? formatTime(rows.get(1).totalTimeMs) : "--");
                        }
                        if (rows.size() > 2) {
                            thirdName.setText(rows.get(2).username);
                            thirdScore.setText(rows.get(2).hasCompleted ? rows.get(2).correct + " pts" : "Not completed");
                            thirdTime.setText(rows.get(2).hasCompleted ? formatTime(rows.get(2).totalTimeMs) : "--");
                        }
                        
                        // Adjust bar heights based on score (only for completed quizzes)
                        if (rows.size() >= 3) {
                            int firstScore = rows.get(0).hasCompleted ? rows.get(0).correct : 0;
                            int secondScore = rows.get(1).hasCompleted ? rows.get(1).correct : 0;
                            int thirdScore = rows.get(2).hasCompleted ? rows.get(2).correct : 0;
                            adjustBarHeights(firstScore, secondScore, thirdScore);
                        }
                        
                        // Populate other players section (rank 4+) with detailed timing
                        otherPlayersList.getChildren().clear();
                        for (int i = 3; i < rows.size(); i++) {
                            ResultRow row = rows.get(i);
                            VBox card = createPlayerCard(i + 1, row);
                            otherPlayersList.getChildren().add(card);
                        }
                    });
                } else {
                    System.err.println("[LeaderboardController] Failed to fetch leaderboard: " + resp.statusCode());
                    System.err.println("[LeaderboardController] Response: " + resp.body());
                }
            } catch (Exception e) {
                System.err.println("[LeaderboardController] Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private List<AnswerDetail> fetchAnswerDetails(String username) {
        try {
            String url = ServerConfig.getBackendUrl() + "/api/answers?room=" + roomId + "&username=" + java.net.URLEncoder.encode(username, "UTF-8");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (resp.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                List<java.util.Map<String,Object>> list = mapper.readValue(resp.body(), new TypeReference<List<java.util.Map<String,Object>>>(){});
                
                return list.stream().map(m -> {
                    int questionIndex = Integer.parseInt(java.util.Objects.toString(m.getOrDefault("question_index", "0")));
                    String selectedAnswer = java.util.Objects.toString(m.getOrDefault("selected_answer", ""));
                    long timeMs = Long.parseLong(java.util.Objects.toString(m.getOrDefault("time_ms", "0")));
                    boolean isCorrect = Integer.parseInt(java.util.Objects.toString(m.getOrDefault("is_correct", "0"))) == 1;
                    
                    return new AnswerDetail(questionIndex, selectedAnswer, timeMs, isCorrect);
                }).collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("[LeaderboardController] Failed to fetch answer details for " + username + ": " + e.getMessage());
        }
        return new java.util.ArrayList<>();
    }

    private String formatTime(long totalTimeMs) {
        long seconds = totalTimeMs / 1000;
        long millis = totalTimeMs % 1000;
        if (seconds == 0) {
            return millis + "ms";
        }
        return seconds + "s " + millis + "ms";
    }

    private void adjustBarHeights(int first, int second, int third) {
        // Calculate proportional heights based on scores
        int maxScore = Math.max(first, Math.max(second, third));
        if (maxScore == 0) return;
        
        double bar1Height = 240 * ((double) first / maxScore);
        double bar2Height = 240 * ((double) second / maxScore);
        double bar3Height = 240 * ((double) third / maxScore);
        
        // Set minimum heights
        bar1Height = Math.max(bar1Height, 180);
        bar2Height = Math.max(bar2Height, 120);
        bar3Height = Math.max(bar3Height, 100);
        
        bar1Container.setMinHeight(bar1Height);
        bar2Container.setMinHeight(bar2Height);
        bar3Container.setMinHeight(bar3Height);
    }

    private VBox createPlayerCard(int rank, ResultRow row) {
        VBox card = new VBox(8);
        String baseStyle = "-fx-padding: 16; -fx-background-color: white; -fx-background-radius: 16; " +
                "-fx-border-color: #2C1810; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);";
        
        // Different styling for incomplete quizzes
        if (!row.hasCompleted) {
            baseStyle = "-fx-padding: 16; -fx-background-color: #f8f9fa; -fx-background-radius: 16; " +
                    "-fx-border-color: #dee2e6; -fx-border-width: 1; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 10, 0, 0, 2);";
        }
        
        card.setStyle(baseStyle);
        
        HBox topRow = new HBox(12);
        topRow.setStyle("-fx-alignment: center-left;");
        
        Label rankLabel = new Label(String.format("%02d", rank));
        rankLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #667eea; -fx-min-width: 35;");
        
        VBox userInfo = new VBox(2);
        Label nameLabel = new Label(row.username);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2C1810;");
        
        String timeText = row.hasCompleted ? "Time: " + formatTime(row.totalTimeMs) : "Status: Not completed";
        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (row.hasCompleted ? "#999" : "#dc3545") + ";");
        userInfo.getChildren().addAll(nameLabel, timeLabel);
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        String pointsText = row.hasCompleted ? row.correct + " pts" : "Pending";
        String pointsStyle = row.hasCompleted ? 
            "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #10B981; -fx-padding: 6 12; " +
            "-fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 8;" :
            "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #6c757d; -fx-padding: 6 12; " +
            "-fx-background-color: rgba(108, 117, 125, 0.1); -fx-background-radius: 8;";
        
        Label pointsLabel = new Label(pointsText);
        pointsLabel.setStyle(pointsStyle);
        
        topRow.getChildren().addAll(rankLabel, userInfo, spacer, pointsLabel);
        card.getChildren().add(topRow);
        
        return card;
    }

    public static class ResultRow {
        public String username;
        public int correct;
        public long totalTimeMs;
        public List<AnswerDetail> answerDetails;
        public boolean hasCompleted;
        
        public ResultRow() {}
        public ResultRow(String username, int correct, long totalTimeMs) {
            this(username, correct, totalTimeMs, new java.util.ArrayList<>(), true);
        }
        public ResultRow(String username, int correct, long totalTimeMs, List<AnswerDetail> answerDetails) {
            this(username, correct, totalTimeMs, answerDetails, true);
        }
        public ResultRow(String username, int correct, long totalTimeMs, List<AnswerDetail> answerDetails, boolean hasCompleted) {
            this.username = username; 
            this.correct = correct; 
            this.totalTimeMs = totalTimeMs; 
            this.answerDetails = answerDetails;
            this.hasCompleted = hasCompleted;
        }
    }
    
    public static class AnswerDetail {
        public int questionIndex;
        public String selectedAnswer;
        public long timeMs;
        public boolean isCorrect;
        
        public AnswerDetail(int questionIndex, String selectedAnswer, long timeMs, boolean isCorrect) {
            this.questionIndex = questionIndex;
            this.selectedAnswer = selectedAnswer;
            this.timeMs = timeMs;
            this.isCorrect = isCorrect;
        }
    }
}
