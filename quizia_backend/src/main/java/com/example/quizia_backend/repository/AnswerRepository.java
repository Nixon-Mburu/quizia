package com.example.quizia_backend.repository;

import com.example.quizia_backend.model.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class AnswerRepository {
    private final JdbcTemplate jdbc;

    public AnswerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int save(Result.Answer a) {
        String sql = "INSERT INTO answers(room_id, username, question_index, selected_answer, time_ms, is_correct) VALUES(?,?,?,?,?,?)";
        return jdbc.update(sql, a.getRoomId(), a.getUsername(), a.getQuestionIndex(), a.getSelectedAnswer(), a.getTimeMs(), a.isCorrect() ? 1 : 0);
    }

    public List<Result.Answer> findByRoomIdAndUsername(String roomId, String username) {
        String sql = "SELECT id, room_id, username, question_index, selected_answer, time_ms, is_correct FROM answers WHERE room_id = ? AND username = ? ORDER BY question_index ASC";
        return jdbc.query(sql, new Object[]{roomId, username}, new RowMapper<Result.Answer>() {
            @Override
            public Result.Answer mapRow(ResultSet rs, int rowNum) throws SQLException {
                Result.Answer a = new Result.Answer();
                a.setId(rs.getInt("id"));
                a.setRoomId(rs.getString("room_id"));
                a.setUsername(rs.getString("username"));
                a.setQuestionIndex(rs.getInt("question_index"));
                a.setSelectedAnswer(rs.getString("selected_answer"));
                a.setTimeMs(rs.getLong("time_ms"));
                a.setCorrect(rs.getInt("is_correct") == 1);
                return a;
            }
        });
    }
}