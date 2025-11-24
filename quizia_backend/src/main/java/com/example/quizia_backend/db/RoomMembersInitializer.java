package com.example.quizia_backend.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Component
public class RoomMembersInitializer {
    private final JdbcTemplate jdbc;

    public RoomMembersInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS room_members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "room_id TEXT NOT NULL, " +
                "username TEXT NOT NULL, " +
                "joined_at TEXT NOT NULL" +
                ");";
        jdbc.execute(sql);
    }
}