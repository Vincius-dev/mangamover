package com.mangamover.service;

import com.mangamover.db.Database;
import com.mangamover.model.Job;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobServiceTest {

    private Database db;
    private JobService jobService;

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(":memory:");
        jobService = new JobService(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        db.getConnection().close();
    }

    private Job newJob(String name, String src, String dest, boolean watch, boolean active) {
        return newJob(name, src, dest, watch, active, false);
    }

    private Job newJob(String name, String src, String dest, boolean watch, boolean active, boolean recursive) {
        Job j = new Job();
        j.name = name;
        j.sourcePath = src;
        j.destPath = dest;
        j.watch = watch;
        j.active = active;
        j.recursive = recursive;
        return j;
    }

    @Test
    void create_returnsJobWithGeneratedId() throws SQLException {
        Job job = newJob("Manga Job", "/src", "/dest", true, true);
        Job created = jobService.create(job);

        assertTrue(created.id > 0);
        assertEquals("Manga Job", created.name);
        assertEquals("/src", created.sourcePath);
        assertEquals("/dest", created.destPath);
        assertTrue(created.watch);
        assertTrue(created.active);
    }

    @Test
    void create_setsCreatedAt() throws SQLException {
        Job created = jobService.create(newJob("Job", "/s", "/d", false, true));
        assertNotNull(created.createdAt);
        assertFalse(created.createdAt.isBlank());
    }

    @Test
    void findById_returnsCorrectJob() throws SQLException {
        Job created = jobService.create(newJob("Job A", "/a", "/b", true, false));
        Job found = jobService.findById(created.id);

        assertNotNull(found);
        assertEquals(created.id, found.id);
        assertEquals("Job A", found.name);
    }

    @Test
    void findById_notFound_returnsNull() throws SQLException {
        Job found = jobService.findById(9999L);
        assertNull(found);
    }

    @Test
    void findAll_returnsAllJobs() throws SQLException {
        jobService.create(newJob("Job 1", "/s1", "/d1", true, true));
        jobService.create(newJob("Job 2", "/s2", "/d2", false, false));
        jobService.create(newJob("Job 3", "/s3", "/d3", true, false));

        List<Job> all = jobService.findAll();
        assertEquals(3, all.size());
    }

    @Test
    void findAll_empty_returnsEmptyList() throws SQLException {
        List<Job> all = jobService.findAll();
        assertTrue(all.isEmpty());
    }

    @Test
    void findAll_orderedById() throws SQLException {
        Job j1 = jobService.create(newJob("Primeiro", "/s", "/d", true, true));
        Job j2 = jobService.create(newJob("Segundo", "/s", "/d", true, true));
        Job j3 = jobService.create(newJob("Terceiro", "/s", "/d", true, true));

        List<Job> all = jobService.findAll();
        assertEquals(j1.id, all.get(0).id);
        assertEquals(j2.id, all.get(1).id);
        assertEquals(j3.id, all.get(2).id);
    }

    @Test
    void update_changesJobFields() throws SQLException {
        Job created = jobService.create(newJob("Original", "/old-src", "/old-dest", true, true));

        created.name = "Atualizado";
        created.sourcePath = "/new-src";
        created.destPath = "/new-dest";
        created.watch = false;
        created.active = false;

        Job updated = jobService.update(created);

        assertEquals("Atualizado", updated.name);
        assertEquals("/new-src", updated.sourcePath);
        assertEquals("/new-dest", updated.destPath);
        assertFalse(updated.watch);
        assertFalse(updated.active);
    }

    @Test
    void update_persistsChanges() throws SQLException {
        Job created = jobService.create(newJob("Original", "/s", "/d", true, true));
        created.name = "Novo Nome";
        jobService.update(created);

        Job found = jobService.findById(created.id);
        assertEquals("Novo Nome", found.name);
    }

    @Test
    void delete_removesJob() throws SQLException {
        Job created = jobService.create(newJob("Para Deletar", "/s", "/d", true, true));
        boolean deleted = jobService.delete(created.id);

        assertTrue(deleted);
        assertNull(jobService.findById(created.id));
    }

    @Test
    void delete_nonExistentJob_returnsFalse() throws SQLException {
        boolean deleted = jobService.delete(9999L);
        assertFalse(deleted);
    }

    @Test
    void delete_doesNotAffectOtherJobs() throws SQLException {
        Job j1 = jobService.create(newJob("Job 1", "/s", "/d", true, true));
        Job j2 = jobService.create(newJob("Job 2", "/s", "/d", true, true));

        jobService.delete(j1.id);

        assertNull(jobService.findById(j1.id));
        assertNotNull(jobService.findById(j2.id));
        assertEquals(1, jobService.findAll().size());
    }

    @Test
    void create_watchAndActiveFalse_persistCorrectly() throws SQLException {
        Job created = jobService.create(newJob("Job", "/s", "/d", false, false));
        Job found = jobService.findById(created.id);

        assertFalse(found.watch);
        assertFalse(found.active);
    }

    @Test
    void create_recursiveTrue_persistsCorrectly() throws SQLException {
        Job created = jobService.create(newJob("Recursive Job", "/s", "/d", true, true, true));
        Job found = jobService.findById(created.id);

        assertTrue(found.recursive);
    }

    @Test
    void create_recursiveFalseByDefault() throws SQLException {
        Job created = jobService.create(newJob("Job", "/s", "/d", true, true));
        Job found = jobService.findById(created.id);

        assertFalse(found.recursive);
    }

    @Test
    void update_changesRecursiveField() throws SQLException {
        Job created = jobService.create(newJob("Job", "/s", "/d", true, true, false));
        created.recursive = true;
        Job updated = jobService.update(created);

        assertTrue(updated.recursive);
    }

    @Test
    void create_scheduleMinutes_persistsCorrectly() throws SQLException {
        Job j = newJob("Scheduled Job", "/s", "/d", true, true);
        j.scheduleMinutes = 30;
        Job created = jobService.create(j);
        Job found = jobService.findById(created.id);

        assertEquals(30, found.scheduleMinutes);
    }

    @Test
    void create_scheduleMinutesZeroByDefault() throws SQLException {
        Job created = jobService.create(newJob("Job", "/s", "/d", true, true));
        Job found = jobService.findById(created.id);

        assertEquals(0, found.scheduleMinutes);
    }

    @Test
    void update_changesScheduleMinutes() throws SQLException {
        Job created = jobService.create(newJob("Job", "/s", "/d", true, true));
        assertEquals(0, created.scheduleMinutes);

        created.scheduleMinutes = 60;
        Job updated = jobService.update(created);

        assertEquals(60, updated.scheduleMinutes);
    }
}
