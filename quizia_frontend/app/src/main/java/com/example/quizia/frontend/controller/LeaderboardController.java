package com.example.quizia.frontend.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
import java.util.List;
import java.io.IOException;

public class LeaderboardController {
    @FXML private TableView<ResultRow> leaderboardTable;
    @FXML private Button backBtn;
    @FXML private AnchorPane root;

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
                String url = "http://localhost:8081/api/leaderboard?roomId=" + java.net.URLEncoder.encode(roomId, java.nio.charset.StandardCharsets.UTF_8);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<java.util.Map<String,Object>> list = mapper.readValue(resp.body(), new TypeReference<List<java.util.Map<String,Object>>>(){});
                    ObservableList<ResultRow> rows = FXCollections.observableArrayList();
                    for (java.util.Map<String,Object> m : list) {
                        String username = java.util.Objects.toString(m.getOrDefault("username", ""));
                        int correct = Integer.parseInt(java.util.Objects.toString(m.getOrDefault("correct", "0")));
                        long t = Long.parseLong(java.util.Objects.toString(m.getOrDefault("total_time_ms", m.getOrDefault("totalTimeMs", "0"))));
                        rows.add(new ResultRow(username, correct, t));
                    }
                    Platform.runLater(() -> leaderboardTable.setItems(rows));
                } else {
                    System.err.println("Failed to fetch leaderboard: " + resp.statusCode());
                }
            } catch (Exception e) {
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
package com.example.quizia.frontend.controller;

public class LeaderboardController {
    // TODO: Implement leaderboard logic
}
