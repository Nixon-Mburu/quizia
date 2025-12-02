package com.example.quizia.frontend.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.prefs.Preferences;

public class AuthController {
    @FXML private Label logoLabel;
    @FXML private TextField usernameField;
    @FXML private Button createRoomBtn;
    @FXML private Button joinRoomBtn;
    @FXML private AnchorPane root;

    @FXML
    public void initialize() {

        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String saved = prefs.get("quizia.username", "");
        if (saved != null && !saved.isEmpty()) {
            usernameField.setText(saved);
        }


        usernameField.focusedProperty().addListener((obs, wasFocused, isNow) -> {
            if (!isNow) {
                String val = usernameField.getText();
                if (val != null) prefs.put("quizia.username", val);
            }
        });

        createRoomBtn.setOnAction(this::goToRegisterRoom);
        joinRoomBtn.setOnAction(this::goToJoinRoom);
    }

    @FXML
    private void goToRegisterRoom(ActionEvent event) {
        // Ensure username is saved before switching scene
        String username = usernameField.getText().trim();
        if (username != null && !username.isEmpty()) {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            prefs.put("quizia.username", username);
            System.out.println("[DEBUG] AuthController.goToRegisterRoom - Saved username: '" + username + "'");
        }
        switchScene("/fxml/register_room.fxml");
    }

    @FXML
    private void goToJoinRoom(ActionEvent event) {
        // Ensure username is saved before switching scene
        String username = usernameField.getText().trim();
        if (username != null && !username.isEmpty()) {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            prefs.put("quizia.username", username);
            System.out.println("[DEBUG] AuthController.goToJoinRoom - Saved username: '" + username + "'");
        }
        switchScene("/fxml/join_room.fxml");
    }

    private void switchScene(String fxmlPath) {
        try {
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                String msg = "FXML resource not found: " + fxmlPath;
                System.err.println(msg);
                Alert a = new Alert(AlertType.ERROR, msg);
                a.showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent page = loader.load();
            Stage stage = (Stage) root.getScene().getWindow();
            if (stage == null) {
                String msg = "Unable to determine current Stage (root.getScene() returned null)";
                System.err.println(msg);
                Alert a = new Alert(AlertType.ERROR, msg);
                a.showAndWait();
                return;
            }
            stage.setScene(new Scene(page));
        } catch (IOException e) {
            e.printStackTrace();
            Alert a = new Alert(AlertType.ERROR, "Failed to load view: " + fxmlPath + "\n" + e.getMessage());
            a.showAndWait();
        }
    }
}