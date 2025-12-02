package com.example.quizia_backend.repository;

import com.example.quizia_backend.model.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ResultRepository {
    private final JdbcTemplate jdbc;

    public ResultRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int save(Result r) {
        String sql = "INSERT INTO results(room_id, username, correct, total_time_ms) VALUES(?,?,?,?)";
        return jdbc.update(sql, r.getRoomId(), r.getUsername(), r.getCorrect(), r.getTotalTimeMs());
    }

    public List<Result> findByRoomId(String roomId) {
        String sql = "SELECT id, room_id, username, correct, total_time_ms FROM results WHERE room_id = ? ORDER BY correct DESC, total_time_ms ASC";
        return jdbc.query(sql, new Object[]{roomId}, new RowMapper<Result>() {
            @Override
            public Result mapRow(ResultSet rs, int rowNum) throws SQLException {
                Result r = new Result();
                r.setId(rs.getInt("id"));
                r.setRoomId(rs.getString("room_id"));
                r.setUsername(rs.getString("username"));
                r.setCorrect(rs.getInt("correct"));
                r.setTotalTimeMs(rs.getLong("total_time_ms"));
                return r;
            }
        });
    }
}