package com.example.quizia_backend.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class ResultsTableInitializer {
    private final JdbcTemplate jdbc;

    public ResultsTableInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS results (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "room_id TEXT NOT NULL, " +
                "username TEXT NOT NULL, " +
                "correct INTEGER NOT NULL, " +
                "total_time_ms INTEGER NOT NULL" +
                ");";
        jdbc.execute(sql);
    }
}