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
                    
                    // Create result rows
                    List<ResultRow> rows = list.stream().map(m -> {
                        String username = java.util.Objects.toString(m.getOrDefault("username", ""));
                        int correct = Integer.parseInt(java.util.Objects.toString(m.getOrDefault("correct", "0")));
                        long t = Long.parseLong(java.util.Objects.toString(m.getOrDefault("total_time_ms", m.getOrDefault("totalTimeMs", "0"))));
                        return new ResultRow(username, correct, t);
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
                            firstScore.setText(rows.get(0).correct + " pts");
                            firstTime.setText(formatTime(rows.get(0).totalTimeMs));
                        }
                        if (rows.size() > 1) {
                            secondName.setText(rows.get(1).username);
                            secondScore.setText(rows.get(1).correct + " pts");
                            secondTime.setText(formatTime(rows.get(1).totalTimeMs));
                        }
                        if (rows.size() > 2) {
                            thirdName.setText(rows.get(2).username);
                            thirdScore.setText(rows.get(2).correct + " pts");
                            thirdTime.setText(formatTime(rows.get(2).totalTimeMs));
                        }
                        
                        // Adjust bar heights based on score
                        if (rows.size() >= 3) {
                            adjustBarHeights(rows.get(0).correct, rows.get(1).correct, rows.get(2).correct);
                        }
                        
                        // Populate other players section (rank 4+)
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
        card.setStyle("-fx-padding: 16; -fx-background-color: white; -fx-background-radius: 16; " +
                "-fx-border-color: #2C1810; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        
        HBox topRow = new HBox(12);
        topRow.setStyle("-fx-alignment: center-left;");
        
        Label rankLabel = new Label(String.format("%02d", rank));
        rankLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #667eea; -fx-min-width: 35;");
        
        VBox userInfo = new VBox(2);
        Label nameLabel = new Label(row.username);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2C1810;");
        Label timeLabel = new Label("Time: " + formatTime(row.totalTimeMs));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
        userInfo.getChildren().addAll(nameLabel, timeLabel);
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label pointsLabel = new Label(row.correct + " pts");
        pointsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #10B981; -fx-padding: 6 12; " +
                "-fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 8;");
        
        topRow.getChildren().addAll(rankLabel, userInfo, spacer, pointsLabel);
        card.getChildren().add(topRow);
        
        return card;
    }

    public static class ResultRow {
        public String username;
        public int correct;
        public long totalTimeMs;
        public ResultRow() {}
        public ResultRow(String username, int correct, long totalTimeMs) {
            this.username = username; this.correct = correct; this.totalTimeMs = totalTimeMs;
        }
    }
}
