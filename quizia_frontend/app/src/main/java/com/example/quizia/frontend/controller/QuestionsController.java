package com.example.quizia.frontend.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.ArrayList;

import com.example.quizia.frontend.model.Question;

public class QuestionsController {

    private String roomId;
    private String roomName;
    private String topic;

    @FXML
    private Label roomLabel;

    @FXML
    private Label topicLabel;

    @FXML
    private Label questionText;

    @FXML
    private RadioButton optA;

    @FXML
    private RadioButton optB;

    @FXML
    private RadioButton optC;

    @FXML
    private RadioButton optD;

    private ToggleGroup answersGroup = new ToggleGroup();

    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    // store selected answers: index -> selected option letter (A/B/C/D)
    private List<String> selectedAnswers = new ArrayList<>();
    private String username = "";
    private long quizStartTime = 0L;

    @FXML
    private AnchorPane root;

    public void setRoomInfo(String roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
        if (roomLabel != null) {
            roomLabel.setText("Room: " + roomName + " (" + roomId + ")");
        }
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username;
    }

    public void setTopic(String topic) {
        this.topic = topic;
        if (topicLabel != null) {
            topicLabel.setText("Topic: " + topic);
        }

        // fetch questions for the topic
        fetchQuestionsForTopic(topic);
    }

    @FXML
    private void initialize() {
        // reflect any values already set before initialize
        if (roomLabel != null && roomName != null && roomId != null) {
            roomLabel.setText("Room: " + roomName + " (" + roomId + ")");
        }
        if (topicLabel != null && topic != null) {
            topicLabel.setText("Topic: " + topic);
        }
    }

    private void fetchQuestionsForTopic(String topic) {
        // run HTTP request off the UI thread
        new Thread(() -> {
            try {
                String encoded = java.net.URLEncoder.encode(topic, java.nio.charset.StandardCharsets.UTF_8);
                String url = "http://localhost:8081/api/questions?topic=" + encoded + "&limit=30";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<Question> qlist = mapper.readValue(resp.body(), new TypeReference<List<Question>>(){});
                    questions.clear();
                    questions.addAll(qlist);
                    selectedAnswers.clear();
                    for (int i=0;i<questions.size();i++) selectedAnswers.add(null);
                    currentIndex = 0;
                    // start quiz timer
                    quizStartTime = System.currentTimeMillis();
                    Platform.runLater(() -> showQuestion(currentIndex));
                } else {
                    System.err.println("Failed to fetch questions: " + resp.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showQuestion(int index) {
        if (questions.isEmpty() || index < 0 || index >= questions.size()) return;
        Question q = questions.get(index);
        questionText.setText((index+1) + ". " + q.getQuestion());
        optA.setText(q.getOptionA() == null ? "" : q.getOptionA());
        optB.setText(q.getOptionB() == null ? "" : q.getOptionB());
        optC.setText(q.getOptionC() == null ? "" : q.getOptionC());
        optD.setText(q.getOptionD() == null ? "" : q.getOptionD());
        optA.setToggleGroup(answersGroup);
        optB.setToggleGroup(answersGroup);
        optC.setToggleGroup(answersGroup);
        optD.setToggleGroup(answersGroup);
        // restore previously selected answer if any
        String sel = selectedAnswers.get(index);
        answersGroup.selectToggle(null);
        if (sel != null) {
            switch (sel) {
                case "A": answersGroup.selectToggle(optA); break;
                case "B": answersGroup.selectToggle(optB); break;
                case "C": answersGroup.selectToggle(optC); break;
                case "D": answersGroup.selectToggle(optD); break;
            }
        }
    }

    @FXML
    private void onNext(ActionEvent e) {
        saveSelectedForCurrent();
        if (currentIndex < questions.size()-1) {
            currentIndex++;
            showQuestion(currentIndex);
        }
    }

    @FXML
    private void onPrevious(ActionEvent e) {
        saveSelectedForCurrent();
        if (currentIndex > 0) {
            currentIndex--;
            showQuestion(currentIndex);
        }
    }

    private void saveSelectedForCurrent() {
        if (questions.isEmpty()) return;
        String sel = null;
        if (answersGroup.getSelectedToggle() == optA) sel = "A";
        else if (answersGroup.getSelectedToggle() == optB) sel = "B";
        else if (answersGroup.getSelectedToggle() == optC) sel = "C";
        else if (answersGroup.getSelectedToggle() == optD) sel = "D";
        selectedAnswers.set(currentIndex, sel);
    }

    @FXML
    private void onSubmit(ActionEvent e) {
        saveSelectedForCurrent();
        // compute score
        score = 0;
        for (int i=0;i<questions.size();i++) {
            String correct = questions.get(i).getCorrectOption();
            String sel = selectedAnswers.get(i);
            if (correct != null && sel != null && correct.equalsIgnoreCase(sel)) score++;
        }
        long totalMs = System.currentTimeMillis() - quizStartTime;
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Quiz Completed");
        alert.setHeaderText(null);
        alert.setContentText("You scored " + score + " out of " + questions.size() + "\nTime: " + (totalMs/1000.0) + "s");
        alert.showAndWait();

        // submit result to backend
        new Thread(() -> {
            try {
                ResultPayload p = new ResultPayload();
                p.roomId = roomId == null ? "" : roomId;
                p.username = (username == null || username.isEmpty()) ? "anonymous" : username;
                p.correct = score;
                p.totalTimeMs = totalMs;
                ObjectMapper mapper = new ObjectMapper();
                String body = mapper.writeValueAsString(p);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/results"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("Result submit status: " + resp.statusCode() + " body:" + resp.body());
                // after submitting, navigate to leaderboard view
                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leaderboard.fxml"));
                        Parent page = loader.load();
                        LeaderboardController lc = loader.getController();
                        lc.setRoomId(p.roomId);
                        Stage stage = (Stage) root.getScene().getWindow();
                        stage.setScene(new Scene(page));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // simple payload class for submission
    private static class ResultPayload {
        public String roomId;
        public String username;
        public int correct;
        public long totalTimeMs;
    }

    @FXML
    private void onBackToTopics(ActionEvent event) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/fxml/join_room.fxml"));
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(new Scene(page));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
