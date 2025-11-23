package com.example.quizia.frontend.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Parent;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.prefs.Preferences;

public class AuthController {
    @FXML private Label logoLabel;
    @FXML private TextField usernameField;
    @FXML private Button createRoomBtn;
    @FXML private Hyperlink joinRoomBtn;
    @FXML private AnchorPane root;

    @FXML
    public void initialize() {
        // load stored username so user doesn't have to re-enter
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String saved = prefs.get("quizia.username", "");
        if (saved != null && !saved.isEmpty()) {
            usernameField.setText(saved);
        }

        // when username changes, save it
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
        switchScene("/fxml/register_room.fxml");
    }

    @FXML
    private void goToJoinRoom(ActionEvent event) {
        switchScene("/fxml/join_room.fxml");
    }

    private void switchScene(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(page));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
