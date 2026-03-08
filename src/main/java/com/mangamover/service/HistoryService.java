package com.mangamover.service;

import com.mangamover.db.Database;
import com.mangamover.model.HistoryEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryService {
    private final Database db;

    public HistoryService(Database db) {
        this.db = db;
    }

    public void record(long jobId, String filename, String status, String message) throws SQLException {
        synchronized (db) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT INTO history (job_id, filename, status, message) VALUES (?,?,?,?)")) {
                ps.setLong(1, jobId);
                ps.setString(2, filename);
                ps.setString(3, status);
                ps.setString(4, message);
                ps.executeUpdate();
            }
        }
    }

    public List<HistoryEntry> findPaged(int page, int perPage, Long jobId) throws SQLException {
        synchronized (db) {
            String sql = "SELECT id, job_id, filename, status, message, created_at FROM history"
                    + (jobId != null ? " WHERE job_id=?" : "")
                    + " ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                int idx = 1;
                if (jobId != null) ps.setLong(idx++, jobId);
                ps.setInt(idx++, perPage);
                ps.setInt(idx, (page - 1) * perPage);
                ResultSet rs = ps.executeQuery();
                List<HistoryEntry> list = new ArrayList<>();
                while (rs.next()) {
                    HistoryEntry e = new HistoryEntry();
                    e.id = rs.getLong("id");
                    e.jobId = rs.getLong("job_id");
                    e.filename = rs.getString("filename");
                    e.status = rs.getString("status");
                    e.message = rs.getString("message");
                    e.createdAt = rs.getString("created_at");
                    list.add(e);
                }
                return list;
            }
        }
    }

    public long count(Long jobId) throws SQLException {
        synchronized (db) {
            String sql = "SELECT COUNT(*) FROM history" + (jobId != null ? " WHERE job_id=?" : "");
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                if (jobId != null) ps.setLong(1, jobId);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }
}
