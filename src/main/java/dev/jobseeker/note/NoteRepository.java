package dev.jobseeker.note;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository {
    private final JdbcDatabase db;

    public NoteRepository(JdbcDatabase db) {
        this.db = db;
    }

    public List<Map<String, Object>> findByApplication(UUID id) {
        return db.rows(
                "SELECT * FROM notes WHERE application_id=:id ORDER BY created_at,id",
                Map.of("id", id));
    }

    public Map<String, Object> findOwned(UUID id, UUID app) {
        return db.one(
                "SELECT * FROM notes WHERE id=:id AND application_id=:app",
                Map.of("id", id, "app", app));
    }

    public void insert(Map<String, Object> values) {
        db.insert("notes", values);
    }

    public void update(UUID id, Map<String, Object> values) {
        db.updateVersioned("notes", id, values);
    }
}
