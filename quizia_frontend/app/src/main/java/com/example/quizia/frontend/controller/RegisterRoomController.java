package com.example.quizia.frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class RegisterRoomController {

    @FXML
    private TextField roomNameField;

    @FXML
    private TextField roomIdField;


    @FXML
    private Button registerButton;

    @FXML
    private Button backButton;

    @FXML
    private CheckBox topicGeneral;

    @FXML
    private CheckBox topicScience;

    @FXML
    private CheckBox topicPop;

    @FXML
    private CheckBox topicGeo;

    @FXML
    private CheckBox topicSports;

    @FXML
    private CheckBox topicHistory;

    @FXML
    private AnchorPane root;


    @FXML
    private void initialize() {
        registerButton.setOnAction(event -> handleRegisterRoom());
        if (backButton != null) {
            backButton.setOnAction(event -> goToAuthPage());
        }
    }

    private void goToAuthPage() {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/fxml/auth.fxml"));
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(page));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRegisterRoom() {
        String roomName = roomNameField.getText();
        String roomId = roomIdField.getText();
        // send to backend
        new Thread(() -> {
            try {
                Map<String,Object> payload = new HashMap<>();
                payload.put("roomId", roomId);
                payload.put("roomName", roomName);
                payload.put("memberCount", 0);
                payload.put("memberNames", "");
                // collect selected topics
                java.util.List<String> topics = new java.util.ArrayList<>();
                if (topicGeneral != null && topicGeneral.isSelected()) topics.add("General Knowledge");
                if (topicScience != null && topicScience.isSelected()) topics.add("Science & Technology");
                if (topicPop != null && topicPop.isSelected()) topics.add("Pop Culture & Entertainment");
                if (topicGeo != null && topicGeo.isSelected()) topics.add("Geography");
                if (topicSports != null && topicSports.isSelected()) topics.add("Sports");
                if (topicHistory != null && topicHistory.isSelected()) topics.add("History & Politics");
                payload.put("topics", String.join(",", topics));
                ObjectMapper mapper = new ObjectMapper();
                String body = mapper.writeValueAsString(payload);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/rooms/register"))
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Room Registered");
                    alert.setHeaderText(null);
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        alert.setContentText("Room registered successfully");
                        // after successful registration, navigate to join room list so it refreshes from server
                        try {
                            Parent page = FXMLLoader.load(getClass().getResource("/fxml/join_room.fxml"));
                            Stage stage = (Stage) root.getScene().getWindow();
                            stage.setScene(new Scene(page));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        alert.setContentText("Failed to register room: " + resp.statusCode());
                    }
                    alert.showAndWait();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to register room: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
}
