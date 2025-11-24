package com.example.quizia_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RoomMemberRepository {
    private final JdbcTemplate jdbc;

    public RoomMemberRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<java.util.Map<String,Object>> mapper = new RowMapper<java.util.Map<String,Object>>() {
        @Override
        public java.util.Map<String,Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("username", rs.getString("username"));
            m.put("joined_at", rs.getString("joined_at"));
            return m;
        }
    };

    public int addMember(String roomId, String username) {
        String sql = "INSERT INTO room_members(room_id, username, joined_at) VALUES(?, ?, datetime('now'))";
        return jdbc.update(sql, roomId, username);
    }

    public List<java.util.Map<String,Object>> listMembers(String roomId) {
        String sql = "SELECT username, joined_at FROM room_members WHERE room_id = ? ORDER BY joined_at";
        return jdbc.query(sql, new Object[]{roomId}, mapper);
    }

    public int countMembers(String roomId) {
        String sql = "SELECT COUNT(1) FROM room_members WHERE room_id = ?";
        Integer c = jdbc.queryForObject(sql, new Object[]{roomId}, Integer.class);
        return c == null ? 0 : c;
    }
}