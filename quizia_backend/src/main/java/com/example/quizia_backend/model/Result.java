package com.example.quizia_backend.model;

public class Result {
    private int id;
    private String roomId;
    private String username;
    private int correct;
    private long totalTimeMs;

    public Result() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getCorrect() { return correct; }
    public void setCorrect(int correct) { this.correct = correct; }
    public long getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(long totalTimeMs) { this.totalTimeMs = totalTimeMs; }
}
