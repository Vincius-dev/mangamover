package com.mangamover.service;

import com.mangamover.db.Database;
import com.mangamover.model.Job;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JobService {
    private final Database db;

    public JobService(Database db) {
        this.db = db;
    }

    public List<Job> findAll() throws SQLException {
        synchronized (db) {
            List<Job> list = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT id, name, source_path, dest_path, watch, active, recursive, created_at FROM jobs ORDER BY id")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    public Job findById(long id) throws SQLException {
        synchronized (db) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT id, name, source_path, dest_path, watch, active, recursive, created_at FROM jobs WHERE id=?")) {
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Job create(Job job) throws SQLException {
        synchronized (db) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT INTO jobs (name, source_path, dest_path, watch, active, recursive) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, job.name);
                ps.setString(2, job.sourcePath);
                ps.setString(3, job.destPath);
                ps.setInt(4, job.watch ? 1 : 0);
                ps.setInt(5, job.active ? 1 : 0);
                ps.setInt(6, job.recursive ? 1 : 0);
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) job.id = keys.getLong(1);
            }
            return findById(job.id);
        }
    }

    public Job update(Job job) throws SQLException {
        synchronized (db) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "UPDATE jobs SET name=?, source_path=?, dest_path=?, watch=?, active=?, recursive=? WHERE id=?")) {
                ps.setString(1, job.name);
                ps.setString(2, job.sourcePath);
                ps.setString(3, job.destPath);
                ps.setInt(4, job.watch ? 1 : 0);
                ps.setInt(5, job.active ? 1 : 0);
                ps.setInt(6, job.recursive ? 1 : 0);
                ps.setLong(7, job.id);
                ps.executeUpdate();
            }
            return findById(job.id);
        }
    }

    public boolean delete(long id) throws SQLException {
        synchronized (db) {
            try (PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM jobs WHERE id=?")) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        }
    }

    private Job map(ResultSet rs) throws SQLException {
        Job j = new Job();
        j.id = rs.getLong("id");
        j.name = rs.getString("name");
        j.sourcePath = rs.getString("source_path");
        j.destPath = rs.getString("dest_path");
        j.watch = rs.getInt("watch") == 1;
        j.active = rs.getInt("active") == 1;
        j.recursive = rs.getInt("recursive") == 1;
        j.createdAt = rs.getString("created_at");
        return j;
    }
}
