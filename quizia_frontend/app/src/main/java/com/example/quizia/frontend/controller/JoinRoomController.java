package com.example.quizia.frontend.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

public class JoinRoomController {


    @FXML
    private TableView<Room> roomTable;

    @FXML
    private Button backButton;

    @FXML
    private AnchorPane root;

    @FXML
    private void initialize() {
        // Set up columns
        TableColumn<Room, String> idCol = new TableColumn<>("Room ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));

        TableColumn<Room, String> nameCol = new TableColumn<>("Room Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("roomName"));

        TableColumn<Room, Integer> memberNumCol = new TableColumn<>("Member Number");
        memberNumCol.setCellValueFactory(new PropertyValueFactory<>("memberNumber"));

        TableColumn<Room, String> memberNamesCol = new TableColumn<>("Member Names");
        memberNamesCol.setCellValueFactory(new PropertyValueFactory<>("memberNames"));

        TableColumn<Room, Void> joinCol = new TableColumn<>("Join");
        joinCol.setCellFactory(new Callback<TableColumn<Room, Void>, TableCell<Room, Void>>() {
            @Override
            public TableCell<Room, Void> call(final TableColumn<Room, Void> param) {
                return new TableCell<Room, Void>() {
                    private final Button btn = new Button("Join");
                    {
                        btn.setOnAction(event -> {
                            Room room = getTableView().getItems().get(getIndex());
                                    handleJoinRoom(room);
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        });

        roomTable.getColumns().setAll(idCol, nameCol, memberNumCol, memberNamesCol, joinCol);
        // Start column - only enabled for room registrar
        TableColumn<Room, Void> startCol = new TableColumn<>("Start");
        startCol.setCellFactory(new Callback<TableColumn<Room, Void>, TableCell<Room, Void>>() {
            @Override
            public TableCell<Room, Void> call(final TableColumn<Room, Void> param) {
                return new TableCell<Room, Void>() {
                    private final Button btn = new Button("Start Quiz");
                    {
                        btn.setOnAction(event -> {
                            Room room = getTableView().getItems().get(getIndex());
                            handleStartRoom(room);
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            // show button only if current user is registrar
                            String currentUser = "";
                            try {
                                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(getClass());
                                currentUser = prefs.get("quizia.username", "");
                            } catch (Exception ex) { }
                            Room r = getTableView().getItems().get(getIndex());
                            boolean isRegistrar = r.getCreatedByUsername() != null && !r.getCreatedByUsername().isEmpty() && r.getCreatedByUsername().equals(currentUser);
                            btn.setVisible(isRegistrar);
                            btn.setManaged(isRegistrar);
                            setGraphic(btn);
                        }
                    }
                };
            }
        });
        // append start column at end
        roomTable.getColumns().add(startCol);
        // load rooms from backend
        fetchRoomsFromBackend();
        if (backButton != null) {
            backButton.setOnAction(event -> goToAuthPage());
        }

        // Refresh room list when the scene becomes visible (e.g., after registering a room)
        root.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin != null) {
                        // when window is shown, refresh rooms
                        fetchRoomsFromBackend();
                    }
                });
            }
        });
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

    private ObservableList<Room> getMockRooms() {
        return FXCollections.observableArrayList(
            new Room("101", "Alpha", 3, "Alice, Bob, Carol"),
            new Room("102", "Beta", 2, "Dave, Eve"),
            new Room("103", "Gamma", 4, "Frank, Grace, Heidi, Ivan"),
            new Room("104", "Delta", 1, "Judy")
        );
    }

    private void fetchRoomsFromBackend() {
        new Thread(() -> {
            try {
                String url = "http://localhost:8081/api/rooms";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    List<java.util.Map<String,Object>> list = mapper.readValue(resp.body(), new TypeReference<List<java.util.Map<String,Object>>>(){});
                    ObservableList<Room> items = FXCollections.observableArrayList();
                    for (java.util.Map<String,Object> m : list) {
                        String rid = java.util.Objects.toString(m.getOrDefault("roomId", m.getOrDefault("room_id", "")), "");
                        String rname = java.util.Objects.toString(m.getOrDefault("roomName", m.getOrDefault("room_name", "")), "");
                        int mc = 0;
                        try {
                            Object mcObj = m.getOrDefault("memberCount", m.getOrDefault("member_count", 0));
                            if (mcObj != null) mc = Integer.parseInt(java.util.Objects.toString(mcObj, "0"));
                        } catch (Exception ex) {
                            mc = 0;
                        }
                        String mn = java.util.Objects.toString(m.getOrDefault("memberNames", m.getOrDefault("member_names", "")), "");
                        String topics = java.util.Objects.toString(m.getOrDefault("topics", ""), "");
                        String createdBy = java.util.Objects.toString(m.getOrDefault("createdByUsername", m.getOrDefault("created_by_username", "")), "");
                        items.add(new Room(rid, rname, mc, mn, topics, createdBy));
                    }
                    Platform.runLater(() -> roomTable.setItems(items));
                } else {
                    System.err.println("Failed to fetch rooms: " + resp.statusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleJoinRoom(Room room) {
        // Call backend to record that this user joined the room, then open Questions page
        new Thread(() -> {
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(getClass());
                String uname = prefs.get("quizia.username", "anonymous");

                // POST /api/rooms/join { roomId, username }
                ObjectMapper mapper = new ObjectMapper();
                java.util.Map<String,String> payload = new java.util.HashMap<>();
                payload.put("roomId", room.getRoomId());
                payload.put("username", uname == null || uname.isEmpty() ? "anonymous" : uname);
                String body = mapper.writeValueAsString(payload);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/rooms/join"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    // navigate to questions on FX thread
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/questions.fxml"));
                            Parent page = loader.load();
                            com.example.quizia.frontend.controller.QuestionsController qc = loader.getController();
                            qc.setRoomInfo(room.getRoomId(), room.getRoomName());
                            qc.setUsername(payload.get("username"));
                            // choose first topic if available
                            String topic = null;
                            if (room.getTopics() != null && !room.getTopics().isEmpty()) {
                                String[] parts = room.getTopics().split(",");
                                if (parts.length > 0) topic = parts[0].trim();
                            }
                            if (topic == null || topic.isEmpty()) topic = "General Knowledge";
                            qc.setTopic(topic);
                            Stage stage = (Stage) root.getScene().getWindow();
                            stage.setScene(new Scene(page));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Navigation Error");
                            alert.setHeaderText(null);
                            alert.setContentText("Could not open questions page: " + ex.getMessage());
                            alert.showAndWait();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Join Failed");
                        alert.setHeaderText(null);
                        alert.setContentText("Could not join room: " + resp.statusCode() + " " + resp.body());
                        alert.showAndWait();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Join Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Error joining room: " + ex.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void handleStartRoom(Room room) {
        // only registrar should click this; call backend and navigate to questions
        new Thread(() -> {
            try {
                java.util.Map<String,String> payload = new java.util.HashMap<>();
                payload.put("roomId", room.getRoomId());
                ObjectMapper mapper = new ObjectMapper();
                String body = mapper.writeValueAsString(payload);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8081/api/rooms/start"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    Platform.runLater(() -> {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/questions.fxml"));
                            Parent page = loader.load();
                            com.example.quizia.frontend.controller.QuestionsController qc = loader.getController();
                            qc.setRoomInfo(room.getRoomId(), room.getRoomName());
                            try {
                                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(getClass());
                                qc.setUsername(prefs.get("quizia.username", ""));
                            } catch (Exception ex) {}
                            String topic = null;
                            if (room.getTopics() != null && !room.getTopics().isEmpty()) {
                                String[] parts = room.getTopics().split(",");
                                if (parts.length > 0) topic = parts[0].trim();
                            }
                            if (topic == null || topic.isEmpty()) topic = "General Knowledge";
                            qc.setTopic(topic);
                            Stage stage = (Stage) root.getScene().getWindow();
                            stage.setScene(new Scene(page));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Start Failed");
                        alert.setHeaderText(null);
                        alert.setContentText("Could not start room: " + resp.statusCode());
                        alert.showAndWait();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // Room class for table data
    public static class Room {
        private String roomId;
        private String roomName;
        private int memberNumber;
        private String memberNames;
        private String topics;
        private String createdByUsername;

            public Room(String roomId, String roomName, int memberNumber, String memberNames) {
                this(roomId, roomName, memberNumber, memberNames, "", "");
            }

            public Room(String roomId, String roomName, int memberNumber, String memberNames, String topics) {
                this(roomId, roomName, memberNumber, memberNames, topics, "");
            }

            public Room(String roomId, String roomName, int memberNumber, String memberNames, String topics, String createdByUsername) {
                this.roomId = roomId;
                this.roomName = roomName;
                this.memberNumber = memberNumber;
                this.memberNames = memberNames;
                this.topics = topics == null ? "" : topics;
                this.createdByUsername = createdByUsername == null ? "" : createdByUsername;
            }
        public String getRoomId() { return roomId; }
        public String getRoomName() { return roomName; }
        public int getMemberNumber() { return memberNumber; }
        public String getMemberNames() { return memberNames; }
        public String getTopics() { return topics; }
        public String getCreatedByUsername() { return createdByUsername; }
    }
}
