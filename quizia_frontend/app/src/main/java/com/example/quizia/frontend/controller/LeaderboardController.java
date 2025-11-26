package com.example.quizia.frontend.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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
    @FXML private TableView<ResultRow> leaderboardTable;
    @FXML private Button backBtn;
    @FXML private AnchorPane root;
    @FXML private Label first;
    @FXML private Label firstScore;
    @FXML private Label second;
    @FXML private Label secondScore;
    @FXML private Label third;
    @FXML private Label thirdScore;

    private String roomId;

    public void setRoomId(String roomId) {
        this.roomId = roomId;
        fetchLeaderboard();
    }

    @FXML
    private void initialize() {
        TableColumn<ResultRow, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        TableColumn<ResultRow, Integer> correctCol = new TableColumn<>("Correct");
        correctCol.setCellValueFactory(new PropertyValueFactory<>("correct"));
        TableColumn<ResultRow, Long> timeCol = new TableColumn<>("Time (ms)");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("totalTimeMs"));
        leaderboardTable.getColumns().setAll(userCol, correctCol, timeCol);

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
                            first.setText(rows.get(0).username);
                            firstScore.setText(rows.get(0).correct + " pts");
                        }
                        if (rows.size() > 1) {
                            second.setText(rows.get(1).username);
                            secondScore.setText(rows.get(1).correct + " pts");
                        }
                        if (rows.size() > 2) {
                            third.setText(rows.get(2).username);
                            thirdScore.setText(rows.get(2).correct + " pts");
                        }
                        
                        // Populate table
                        ObservableList<ResultRow> tableRows = FXCollections.observableArrayList(rows);
                        leaderboardTable.setItems(tableRows);
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

    public static class ResultRow {
        private String username;
        private int correct;
        private long totalTimeMs;
        public ResultRow() {}
        public ResultRow(String username, int correct, long totalTimeMs) {
            this.username = username; this.correct = correct; this.totalTimeMs = totalTimeMs;
        }
        public String getUsername() { return username; }
        public int getCorrect() { return correct; }
        public long getTotalTimeMs() { return totalTimeMs; }
    }
}
