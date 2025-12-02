package com.example.quizia_backend.repository;

import com.example.quizia_backend.model.Question;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionRepository {

    private final JdbcTemplate jdbc;

    public QuestionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Question> mapper = (rs, rowNum) -> {
        Question q = new Question();
        q.setId(rs.getInt("id"));
        q.setTopic(rs.getString("topic"));
        q.setQuestion(rs.getString("question"));
        q.setOptionA(rs.getString("option_a"));
        q.setOptionB(rs.getString("option_b"));
        q.setOptionC(rs.getString("option_c"));
        q.setOptionD(rs.getString("option_d"));
        q.setCorrectOption(rs.getString("correct_option"));
        return q;
    };

    public List<Question> findByTopic(String topic, int limit) {

        String sql = "SELECT * FROM questions WHERE topic = ? ORDER BY RANDOM() LIMIT ?";
        return jdbc.query(sql, mapper, topic, limit);
    }
}