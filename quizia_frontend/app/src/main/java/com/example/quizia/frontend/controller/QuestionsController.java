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
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
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
import com.example.quizia.frontend.config.ServerConfig;

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
    private Label timerLabel;

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

    private List<String> selectedAnswers = new ArrayList<>();
    private String username = "";
    private long quizStartTime = 0L;
    private static final int QUESTION_TIMER_SECONDS = 10;
    private static final int TOTAL_QUESTIONS = 6;
    private javafx.animation.Timeline timerTimeline;


    private Thread sseThread = null;
    private boolean hasAnsweredCurrentQuestion = false;
    private int waitingForOthers = 0;


    private List<AnswerTiming> answerTimings = new ArrayList<>();
    private long currentQuestionStartTime = 0L;

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


        logDebug("setTopic called with topic='" + topic + "' -> fetching questions");
        fetchQuestionsForTopic(topic);
    }

    public void setQuizStartTime(long startTimeMs) {
        this.quizStartTime = startTimeMs;
        logDebug("Quiz start time preset to: " + startTimeMs);
    }

    public void setQuestions(List<Question> questionList) {
        if (questionList == null || questionList.isEmpty()) {
            logDebug("setQuestions called with null/empty list");
            return;
        }
        
        this.questions.clear();
        this.questions.addAll(questionList);
        
        selectedAnswers.clear();
        for (int i = 0; i < questions.size(); i++) selectedAnswers.add(null);
        
        currentIndex = 0;
        
        // Only set quizStartTime if not preset (allows synchronized start)
        if (this.quizStartTime == 0L) {
            quizStartTime = System.currentTimeMillis();
        } else {
            logDebug("Preserving preset quizStartTime: " + this.quizStartTime);
        }
        
        logDebug("setQuestions: loaded " + questions.size() + " questions directly");
        Platform.runLater(() -> showQuestion(currentIndex));
    }

    public void setTopicDeferred(String topic) {
        this.topic = topic;
        if (topicLabel != null) topicLabel.setText("Topic: " + topic);
    }


    public void waitForStart(String roomId) {
        if (roomId == null || roomId.isEmpty()) return;
        new Thread(() -> {
            try {
                String url = ServerConfig.getBackendUrl() + "/api/rooms/" + roomId + "/sse";
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

        if (roomLabel != null && roomName != null && roomId != null) {
            roomLabel.setText("Room: " + roomName + " (" + roomId + ")");
        }
        if (topicLabel != null && topic != null) {
            topicLabel.setText("Topic: " + topic);
        }

        try {
            optA.setToggleGroup(answersGroup);
            optB.setToggleGroup(answersGroup);
            optC.setToggleGroup(answersGroup);
            optD.setToggleGroup(answersGroup);

            optA.setOnAction(evt -> saveSelectedForCurrent());
            optB.setOnAction(evt -> saveSelectedForCurrent());
            optC.setOnAction(evt -> saveSelectedForCurrent());
            optD.setOnAction(evt -> saveSelectedForCurrent());
        } catch (Exception ex) {

        }


        if (questionText != null && (questions == null || questions.isEmpty())) {
            questionText.setText("Loading questions...");
        }
    }

    private void fetchQuestionsForTopic(String topic) {

        new Thread(() -> {
            try {
                logDebug("fetchQuestionsForTopic() starting for room=" + roomId);
                String encodedTopic = java.net.URLEncoder.encode(topic, "UTF-8");
                String url = ServerConfig.getBackendUrl() + "/api/questions?room=" + roomId + "&topic=" + encodedTopic;
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
                            questionText.setText("No questions returned for room. Raw response:\n" + raw);
                        });
                        return;
                    }
                    logDebug("parsed questions count: " + qlist.size());
                    questions.clear();
                    
                    // Strictly limit to TOTAL_QUESTIONS (6)
                    int limit = Math.min(TOTAL_QUESTIONS, qlist.size());
                    for (int i = 0; i < limit; i++) {
                        questions.add(qlist.get(i));
                    }
                    
                    logDebug("Using " + questions.size() + " questions (limited to " + TOTAL_QUESTIONS + ")");
                    
                    selectedAnswers.clear();
                    for (int i=0;i<questions.size();i++) selectedAnswers.add(null);
                    currentIndex = 0;

                    // Only set quizStartTime if not preset (allows synchronized start)
                    if (this.quizStartTime == 0L) {
                        quizStartTime = System.currentTimeMillis();
                    } else {
                        logDebug("Preserving preset quizStartTime: " + this.quizStartTime);
                    }
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



    private void logDebug(String msg) {
        try {
            String line = java.time.Instant.now().toString() + " " + msg + "\n";
            java.nio.file.Path p = java.nio.file.Path.of("/tmp/quizia_frontend_debug.log");
            java.nio.file.Files.write(p, line.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable t) {

        }
        System.out.println("[QuestionsController] " + msg);
    }

    private void showQuestion(int index) {
        if (questions.isEmpty() || index < 0 || index >= questions.size()) return;
        Question q = questions.get(index);


        questionText.setText("Question " + (index+1) + " of " + TOTAL_QUESTIONS + "\n" + q.getQuestion());

        optA.setText(q.getOptionA() == null ? "" : q.getOptionA());
        optB.setText(q.getOptionB() == null ? "" : q.getOptionB());
        optC.setText(q.getOptionC() == null ? "" : q.getOptionC());
        optD.setText(q.getOptionD() == null ? "" : q.getOptionD());


        hasAnsweredCurrentQuestion = false;
        waitingForOthers = 0;


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


        currentQuestionStartTime = System.currentTimeMillis();


        startQuestionTimer();


        if (index == 0 && sseThread == null) {
            startSyncListener();
        }
    }

    private void startQuestionTimer() {

        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        if (timerLabel == null) return;


        final int[] secondsRemaining = {QUESTION_TIMER_SECONDS};
        timerTimeline = new Timeline();
        timerTimeline.setCycleCount(QUESTION_TIMER_SECONDS + 1);

        KeyFrame keyFrame = new KeyFrame(
            Duration.seconds(1),
            event -> {
                timerLabel.setText(secondsRemaining[0] + "s");
                if (secondsRemaining[0] <= 3) {
                    timerLabel.setStyle("-fx-text-fill: #ef4444;");
                } else {
                    timerLabel.setStyle("-fx-text-fill: #10B981;");
                }
                secondsRemaining[0]--;
            }
        );

        timerTimeline.getKeyFrames().add(keyFrame);
        timerLabel.setText(QUESTION_TIMER_SECONDS + "s");
        
        // Auto-advance when timer finishes
        timerTimeline.setOnFinished(event -> {
            logDebug("Timer finished for question " + (currentIndex + 1));
            if (currentIndex < questions.size() - 1) {
                currentIndex++;
                showQuestion(currentIndex);
            } else {
                autoSubmitQuiz();
            }
        });
        
        // If a preset quizStartTime exists and is in the future, delay the timer start
        long now = System.currentTimeMillis();
        long delayMs = (quizStartTime > now) ? (quizStartTime - now) : 0L;
        if (delayMs > 0) {
            logDebug("Delaying question timer start by " + delayMs + "ms (sync start)");
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(Duration.millis(delayMs));
            pt.setOnFinished(ev -> timerTimeline.play());
            pt.play();
        } else {
            timerTimeline.play();
        }
    }

    @FXML
    private void onNext(ActionEvent e) {

        saveSelectedForCurrent();
    }

    @FXML
    private void onPrevious(ActionEvent e) {

    }

    private void saveSelectedForCurrent() {
        if (questions.isEmpty()) return;
        if (hasAnsweredCurrentQuestion) return;

        String sel = null;
        if (answersGroup.getSelectedToggle() == optA) sel = "A";
        else if (answersGroup.getSelectedToggle() == optB) sel = "B";
        else if (answersGroup.getSelectedToggle() == optC) sel = "C";
        else if (answersGroup.getSelectedToggle() == optD) sel = "D";

        if (sel == null) return;

        selectedAnswers.set(currentIndex, sel);
        hasAnsweredCurrentQuestion = true;


        long answerTime = System.currentTimeMillis();
        long timeTaken = answerTime - currentQuestionStartTime;

        AnswerTiming timing = new AnswerTiming();
        timing.questionIndex = currentIndex;
        timing.selectedOption = sel;
        timing.timeMs = timeTaken;
        timing.timestamp = answerTime;
        answerTimings.add(timing);

        logDebug("Answer recorded: Q" + (currentIndex+1) + " = " + sel + " in " + timeTaken + "ms");


        submitAnswer(currentIndex, sel, timeTaken);
    }

    private void submitAnswer(int questionIndex, String answer, long timeMs) {
        new Thread(() -> {
            try {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("roomId", roomId);
                payload.put("username", username);
                payload.put("questionIndex", questionIndex);
                payload.put("answer", answer);
                payload.put("timeMs", timeMs);

                ObjectMapper mapper = new ObjectMapper();
                String body = mapper.writeValueAsString(payload);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.getBackendUrl() + "/api/answers/submit"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                logDebug("Answer submit status: " + resp.statusCode());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    Platform.runLater(() -> {
                        waitingForOthers++;

                        if (timerLabel != null) {
                            timerLabel.setText("Waiting...");
                            timerLabel.setStyle("-fx-text-fill: #667eea;");
                        }
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void startSyncListener() {
        sseThread = new Thread(() -> {
            try {
                String url = ServerConfig.getBackendUrl() + "/api/rooms/" + roomId + "/sync";
                logDebug("Starting SSE sync listener: " + url);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                client.sendAsync(req, HttpResponse.BodyHandlers.ofLines())
                    .thenAccept(resp -> {
                        resp.body().forEach(line -> {
                            if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();
                                logDebug("SSE event: " + data);

                                if (data.contains("NEXT_QUESTION")) {

                                    Platform.runLater(() -> {
                                        if (currentIndex < questions.size() - 1) {
                                            currentIndex++;
                                            showQuestion(currentIndex);
                                        } else {

                                            autoSubmitQuiz();
                                        }
                                    });
                                }
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        sseThread.setDaemon(true);
        sseThread.start();
    }

    private void autoSubmitQuiz() {

        long cumulativeTimeMs = 0;
        for (AnswerTiming timing : answerTimings) {
            cumulativeTimeMs += timing.timeMs;
        }

        score = 0;
        for (int i = 0; i < questions.size(); i++) {
            String correct = questions.get(i).getCorrectOption();
            String sel = selectedAnswers.get(i);
            if (correct != null && sel != null && correct.equalsIgnoreCase(sel)) {
                score++;
            }
        }

        logDebug("Quiz complete: " + score + "/" + questions.size() + " in " + cumulativeTimeMs + "ms");


        submitResults(cumulativeTimeMs);
    }

    private void submitResults(long cumulativeTimeMs) {
        new Thread(() -> {
            try {
                ResultPayload p = new ResultPayload();
                p.roomId = roomId == null ? "" : roomId;
                p.username = (username == null || username.isEmpty()) ? "anonymous" : username;
                p.correct = score;
                p.totalTimeMs = cumulativeTimeMs;

                ObjectMapper mapper = new ObjectMapper();
                String body = mapper.writeValueAsString(p);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.getBackendUrl() + "/api/results/submit"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                logDebug("Result submit status: " + resp.statusCode());


                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/leaderboard.fxml"));
                        Parent page = loader.load();
                        LeaderboardController lc = loader.getController();
                        lc.setRoomId(roomId);
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

    @FXML
    private void onSubmit(ActionEvent e) {

        saveSelectedForCurrent();
    }


    private static class ResultPayload {
        public String roomId;
        public String username;
        public int correct;
        public long totalTimeMs;
    }


    private static class AnswerTiming {
        public int questionIndex;
        public String selectedOption;
        public long timeMs;
        public long timestamp;
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