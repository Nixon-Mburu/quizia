package com.example.quizia_backend.repository;

import com.example.quizia_backend.model.Room;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoomRepository {

    private final JdbcTemplate jdbc;

    public RoomRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Room> mapper = (rs, rowNum) -> {
        Room r = new Room();
        r.setRoomId(rs.getString("room_id"));
        r.setRoomName(rs.getString("room_name"));
        r.setMemberCount(rs.getInt("member_count"));
        r.setMemberNames(rs.getString("member_names"));
        try {
            r.setTopics(rs.getString("topics"));
        } catch (Exception ex) {
            r.setTopics(null);
        }
        r.setCreatedAt(rs.getString("created_at"));
        try {
            r.setCreatedByUsername(rs.getString("created_by_username"));
        } catch (Exception ex) {
            r.setCreatedByUsername(null);
        }
        return r;
    };

    public List<Room> findAll() {
        String sql = "SELECT rr.* FROM registered_rooms rr ORDER BY rr.created_at DESC";
        return jdbc.query(sql, mapper);
    }

    public int addRoom(Room room) {
        String sql = "INSERT OR REPLACE INTO registered_rooms (room_id, room_name, member_count, member_names, topics, created_by_username, created_at) VALUES (?, ?, ?, ?, ?, ?, datetime('now'))";

        String mids = room.getMemberNames();
        if (mids == null) mids = "";
        String topics = room.getTopics();
        if (topics == null) topics = "";
        String createdBy = room.getCreatedByUsername();
        if (createdBy == null) createdBy = "";
        return jdbc.update(sql, room.getRoomId(), room.getRoomName(), room.getMemberCount(), mids, topics, createdBy);
    }
}