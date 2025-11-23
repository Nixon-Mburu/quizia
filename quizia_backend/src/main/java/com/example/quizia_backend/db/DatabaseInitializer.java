package com.example.quizia_backend.db;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseInitializer {

    private final DataSource dataSource;

    @Value("classpath:db/quizia.sql")
    private Resource schemaSql;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() throws SQLException, IOException {
        try (Connection conn = dataSource.getConnection()) {
            // Run the SQL script to create tables and seed data if not present
            ScriptUtils.executeSqlScript(conn, schemaSql);
            // Ensure `topics` column exists on registered_rooms for backward compatibility
            boolean hasTopics = false;
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("PRAGMA table_info(registered_rooms);")) {
                while (rs.next()) {
                    String colName = rs.getString("name");
                    if ("topics".equalsIgnoreCase(colName)) {
                        hasTopics = true;
                        break;
                    }
                }
            }
            if (!hasTopics) {
                try (var stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE registered_rooms ADD COLUMN topics TEXT;");
                }
            }
        }
    }
}
