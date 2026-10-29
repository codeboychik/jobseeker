package dev.jobseeker.application;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationReferenceRepository {
    private final JdbcDatabase db;

    public ApplicationReferenceRepository(JdbcDatabase db) {
        this.db = db;
    }

    public void round(UUID app, Object record) {
        if (record != null)
            db.one(
                    "SELECT id FROM interview_rounds WHERE id=:record AND application_id=:app",
                    Map.of("record", record, "app", app));
    }

    public void note(UUID app, Object record) {
        if (record != null)
            db.one(
                    "SELECT id FROM notes WHERE id=:record AND application_id=:app",
                    Map.of("record", record, "app", app));
    }

    public void reason(UUID app, Object record) {
        if (record != null)
            db.one(
                    "SELECT id FROM rejection_reasons WHERE id=:record AND application_id=:app",
                    Map.of("record", record, "app", app));
    }

    public void question(UUID app, Object record) {
        if (record != null)
            db.one(
                    "SELECT q.id FROM questions q JOIN interview_rounds r ON r.id=q.round_id WHERE"
                            + " q.id=:record AND r.application_id=:app",
                    Map.of("record", record, "app", app));
    }

    public void roundType(Object id) {
        db.one("SELECT id FROM round_types WHERE id=:id", Map.of("id", id));
    }

    public void reasonType(Object id) {
        db.one("SELECT id FROM rejection_reason_types WHERE id=:id", Map.of("id", id));
    }
}
