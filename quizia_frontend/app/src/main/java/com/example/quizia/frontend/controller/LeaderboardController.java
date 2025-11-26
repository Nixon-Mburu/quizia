package com.example.quizia.frontend.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    @FXML private Label secondName;
    @FXML private Label secondScore;
    @FXML private Label thirdName;
    @FXML private Label thirdScore;
    @FXML private VBox leaderboardList;

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
                        }
                        if (rows.size() > 1) {
                            secondName.setText(rows.get(1).username);
                            secondScore.setText(rows.get(1).correct + " pts");
                        }
                        if (rows.size() > 2) {
                            thirdName.setText(rows.get(2).username);
                            thirdScore.setText(rows.get(2).correct + " pts");
                        }
                        
                        // Populate leaderboard list (starting from rank 1)
                        leaderboardList.getChildren().clear();
                        for (int i = 0; i < rows.size(); i++) {
                            ResultRow row = rows.get(i);
                            HBox rowBox = createLeaderboardRow(i + 1, row);
                            leaderboardList.getChildren().add(rowBox);
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

    private HBox createLeaderboardRow(int rank, ResultRow row) {
        HBox rowBox = new HBox(12);
        rowBox.setStyle("-fx-padding: 12 16; -fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 8; -fx-alignment: center-left;");
        
        // Rank number
        Label rankLabel = new Label(String.format("%02d", rank));
        rankLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: white; -fx-min-width: 30;");
        
        // User info section
        VBox userInfo = new VBox(2);
        Label nameLabel = new Label(row.username);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: white;");
        Label pointsLabel = new Label(row.correct + " points");
        pointsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255, 255, 255, 0.8);");
        userInfo.getChildren().addAll(nameLabel, pointsLabel);
        
        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Crown icon for top 3
        Label crownLabel = new Label("");
        if (rank == 1) crownLabel.setText("👑");
        else if (rank == 2) crownLabel.setText("🥈");
        else if (rank == 3) crownLabel.setText("🥉");
        crownLabel.setStyle("-fx-font-size: 16px; -fx-min-width: 30;");
        
        rowBox.getChildren().addAll(rankLabel, userInfo, spacer, crownLabel);
        return rowBox;
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
