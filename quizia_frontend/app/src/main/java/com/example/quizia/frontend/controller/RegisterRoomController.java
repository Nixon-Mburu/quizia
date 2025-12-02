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
import com.example.quizia.frontend.config.ServerConfig;

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
        System.out.println("[RegisterRoomController] Initializing - registerButton: " + (registerButton != null));
        System.out.println("[RegisterRoomController] Initializing - backButton: " + (backButton != null));
        System.out.println("[RegisterRoomController] Initializing - root: " + (root != null));
        if (registerButton != null) {
            registerButton.setVisible(true);
            registerButton.setOnAction(event -> handleRegisterRoom());
            System.out.println("[RegisterRoomController] registerButton text: " + registerButton.getText());
        }
        if (backButton != null) {
            backButton.setVisible(true);
            backButton.setOnAction(event -> goToAuthPage());
            System.out.println("[RegisterRoomController] backButton text: " + backButton.getText());
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

    @FXML
    private void handleRegisterRoom() {
        System.out.println("[RegisterRoomController] handleRegisterRoom called");
        String roomName = roomNameField.getText();
        String roomId = roomIdField.getText();


        if (roomName == null || roomName.trim().isEmpty()) {
            showError("Room Name Required", "Please enter a room name.");
            return;
        }


        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(getClass());
        String username = prefs.get("quizia.username", "");
        if (username.isEmpty()) {
            showError("Username Required", "Please set your username first on the auth page.");
            return;
        }


        final String finalRoomId = (roomId == null || roomId.trim().isEmpty()) ? ("room_" + System.currentTimeMillis()) : roomId;


        new Thread(() -> {
            try {
                Map<String,Object> payload = new HashMap<>();
                payload.put("roomId", finalRoomId);
                payload.put("roomName", roomName);
                payload.put("createdByUsername", username);


                java.util.List<String> topics = new java.util.ArrayList<>();
                if (topicGeneral != null && topicGeneral.isSelected()) topics.add("General Knowledge");
                if (topicScience != null && topicScience.isSelected()) topics.add("Science & Technology");
                if (topicPop != null && topicPop.isSelected()) topics.add("Pop Culture & Entertainment");
                if (topicGeo != null && topicGeo.isSelected()) topics.add("Geography");
                if (topicSports != null && topicSports.isSelected()) topics.add("Sports");
                if (topicHistory != null && topicHistory.isSelected()) topics.add("History & Politics");

                if (topics.isEmpty()) {
                    Platform.runLater(() -> showError("Topic Required", "Please select at least one topic."));
                    return;
                }

                payload.put("topics", String.join(",", topics));
                ObjectMapper mapper = new ObjectMapper();
                String body = mapper.writeValueAsString(payload);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.getBackendUrl() + "/api/rooms/register"))
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Room Registered");
                        alert.setHeaderText(null);
                        alert.setContentText("Room '" + roomName + "' registered successfully!\nRoom ID: " + finalRoomId);
                        alert.showAndWait();

                        try {
                            Parent page = FXMLLoader.load(getClass().getResource("/fxml/join_room.fxml"));
                            Stage stage = (Stage) root.getScene().getWindow();
                            stage.setScene(new Scene(page));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        showError("Registration Failed", "Failed to register room (status " + resp.statusCode() + "): " + resp.body());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Error", "Failed to register room: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}