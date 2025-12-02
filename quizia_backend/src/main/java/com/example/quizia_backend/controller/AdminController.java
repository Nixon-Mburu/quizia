package com.example.quizia_backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final DataSource dataSource;

    @Value("classpath:db/quizia.sql")
    private Resource schemaSql;

    public AdminController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostMapping("/reload-db")
    public ResponseEntity<String> reloadDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, schemaSql);
            return ResponseEntity.ok("Reloaded DB from quizia.sql");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to reload DB: " + e.getMessage());
        }
    }
}