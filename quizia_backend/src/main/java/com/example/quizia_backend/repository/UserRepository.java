package com.example.quizia_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<java.util.Map<String,Object>> mapper = new RowMapper<java.util.Map<String,Object>>() {
        @Override
        public java.util.Map<String,Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("user_id", rs.getInt("user_id"));
            m.put("username", rs.getString("username"));
            return m;
        }
    };

    public int addUser(String username) {
        String sql = "INSERT OR IGNORE INTO users (username) VALUES (?)";
        return jdbc.update(sql, username);
    }

    public List<java.util.Map<String,Object>> findAll() {
        String sql = "SELECT user_id, username FROM users ORDER BY user_id";
        return jdbc.query(sql, mapper);
    }
}