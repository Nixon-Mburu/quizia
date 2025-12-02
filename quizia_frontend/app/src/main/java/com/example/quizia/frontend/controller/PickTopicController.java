package com.example.quizia.frontend.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.io.IOException;

public class PickTopicController {

    @FXML
    private AnchorPane root;

    private String selectedRoomId;
    private String selectedRoomName;

    @FXML
    private void onPickTopic(ActionEvent event) {

        String topic = "";
        if (event.getSource() instanceof javafx.scene.control.Hyperlink) {
            javafx.scene.control.Hyperlink link = (javafx.scene.control.Hyperlink) event.getSource();
            topic = link.getText();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/questions.fxml"));
            Parent page = loader.load();
            QuestionsController qc = loader.getController();
            qc.setRoomInfo(selectedRoomId, selectedRoomName);
            qc.setTopic(topic);
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(page));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private javafx.scene.control.Button backButton;

    @FXML
    private void onBack(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/join_room.fxml"));
            Parent page = loader.load();
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(page));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRoomInfo(String roomId, String roomName) {
        this.selectedRoomId = roomId;
        this.selectedRoomName = roomName;
    }
}