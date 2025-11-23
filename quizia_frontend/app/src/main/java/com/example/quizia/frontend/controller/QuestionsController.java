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

        // by default fetch immediately (used when registrar starts)
        logDebug("setTopic called with topic='" + topic + "' -> fetching questions");
        fetchQuestionsForTopic(topic);
    }

    // set topic without immediately fetching - used for deferred start wait
    public void setTopicDeferred(String topic) {
        this.topic = topic;
        if (topicLabel != null) topicLabel.setText("Topic: " + topic);
    }

    // Subscribe to server-sent events for the room and start quiz when a 'start' event arrives
    public void waitForStart(String roomId) {
        if (roomId == null || roomId.isEmpty()) return;
        new Thread(() -> {
            try {
                String url = "http://localhost:8081/api/rooms/" + java.net.URLEncoder.encode(roomId, java.nio.charset.StandardCharsets.UTF_8) + "/events";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<java.io.InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(resp.body()))) {
                        String line;
                        boolean startSeen = false;
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            if (line.startsWith("event:")) {
                                if (line.toLowerCase().contains("start")) {
                                    startSeen = true;
                                }
                            }
                            if (line.startsWith("data:")) {
                                if (line.toLowerCase().contains("start") || line.toLowerCase().contains("started")) {
                                    startSeen = true;
                                }
                            }
                            if (startSeen) {
                                // fetch questions and begin quiz
                                if (this.topic != null) fetchQuestionsForTopic(this.topic);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
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
        // ensure radio buttons are in the same ToggleGroup and set click handlers
        try {
            optA.setToggleGroup(answersGroup);
            optB.setToggleGroup(answersGroup);
            optC.setToggleGroup(answersGroup);
            optD.setToggleGroup(answersGroup);
            // when a radio button is clicked, save the selection for current index
            optA.setOnAction(evt -> saveSelectedForCurrent());
            optB.setOnAction(evt -> saveSelectedForCurrent());
            optC.setOnAction(evt -> saveSelectedForCurrent());
            optD.setOnAction(evt -> saveSelectedForCurrent());
        } catch (Exception ex) {
            // defensive: if controls not yet injected, ignore
        }

        // show placeholder until questions load
        if (questionText != null && (questions == null || questions.isEmpty())) {
            questionText.setText("Loading questions...");
        }
    }

    private void fetchQuestionsForTopic(String topic) {
        // run HTTP request off the UI thread
        new Thread(() -> {
            try {
                logDebug("fetchQuestionsForTopic() starting for topic='" + topic + "'");
                String encoded = java.net.URLEncoder.encode(topic, java.nio.charset.StandardCharsets.UTF_8);
                String url = "http://localhost:8081/api/questions?topic=" + encoded + "&limit=30";
                logDebug("GET " + url);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                logDebug("response status: " + resp.statusCode());
                if (resp.statusCode() == 200) {
                    String body = resp.body();
                    logDebug("response length: " + (body == null ? 0 : body.length()));
                    ObjectMapper mapper = new ObjectMapper();
                    List<Question> qlist = null;
                    try {
                        qlist = mapper.readValue(body, new TypeReference<List<Question>>(){});
                        // randomize question order client-side to ensure different order per user
                        try {
                            java.util.Collections.shuffle(qlist);
                        } catch (Exception ex) {
                            // ignore shuffle failures
                        }
                        System.out.println("[QuestionsController] parsed questions count: " + (qlist == null ? 0 : qlist.size()));
                    } catch (Exception parseEx) {
                        parseEx.printStackTrace();
                        logDebug("JSON parse error: " + parseEx.getClass().getSimpleName() + ": " + parseEx.getMessage());
                        final String raw = body == null ? "<empty>" : body;
                        Platform.runLater(() -> {
                            questionText.setText("Failed to parse questions JSON. Raw response:\n" + raw);
                        });
                        return;
                    }
                    if (qlist == null || qlist.isEmpty()) {
                        final String raw = body == null ? "<empty>" : body;
                        Platform.runLater(() -> {
                            questionText.setText("No questions returned for topic: " + topic + "\nRaw response:\n" + raw);
                        });
                        return;
                    }
                    logDebug("parsed questions count: " + qlist.size());
                    questions.clear();
                    questions.addAll(qlist);
                    selectedAnswers.clear();
                    for (int i=0;i<questions.size();i++) selectedAnswers.add(null);
                    currentIndex = 0;
                    // start quiz timer
                    quizStartTime = System.currentTimeMillis();
                    Platform.runLater(() -> showQuestion(currentIndex));
                } else {
                    logDebug("Failed to fetch questions: status=" + resp.statusCode());
                    final int status = resp.statusCode();
                    final String body = resp.body();
                    Platform.runLater(() -> {
                        questionText.setText("Failed to fetch questions (status=" + status + ")\nRaw response:\n" + (body == null ? "<empty>" : body));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                logDebug("Exception during fetch: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                final String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                Platform.runLater(() -> {
                    if (questionText != null) questionText.setText("Error fetching questions: " + msg);
                });
            }
        }).start();
    }

    // Append lightweight debug messages to a temp file so they are visible even when console output
    // isn't captured by the Gradle runner. Non-critical — failures are swallowed.
    private void logDebug(String msg) {
        try {
            String line = java.time.Instant.now().toString() + " " + msg + "\n";
            java.nio.file.Path p = java.nio.file.Path.of("/tmp/quizia_frontend_debug.log");
            java.nio.file.Files.write(p, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable t) {
            // ignore
        }
        System.out.println("[QuestionsController] " + msg);
    }

    private void showQuestion(int index) {
        if (questions.isEmpty() || index < 0 || index >= questions.size()) return;
        Question q = questions.get(index);
        questionText.setText((index+1) + ". " + q.getQuestion());
        optA.setText(q.getOptionA() == null ? "" : q.getOptionA());
        optB.setText(q.getOptionB() == null ? "" : q.getOptionB());
        optC.setText(q.getOptionC() == null ? "" : q.getOptionC());
        optD.setText(q.getOptionD() == null ? "" : q.getOptionD());
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
