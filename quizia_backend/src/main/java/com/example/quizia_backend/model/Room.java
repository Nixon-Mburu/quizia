package com.example.quizia_backend.model;

public class Room {
    private String roomId;
    private String roomName;
    private int memberCount;
    private String memberNames;
    private String topics;
    private String createdAt;
    private String createdByUsername;

    public Room() {}

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public String getMemberNames() { return memberNames; }
    public void setMemberNames(String memberNames) { this.memberNames = memberNames; }
    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
}