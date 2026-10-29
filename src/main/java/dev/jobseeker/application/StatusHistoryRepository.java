package dev.jobseeker.application;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class StatusHistoryRepository {
    private final JdbcDatabase db;

    public StatusHistoryRepository(JdbcDatabase db) {
        this.db = db;
    }

    public List<Map<String, Object>> findByApplication(UUID id) {
        return db.rows(
                "SELECT * FROM status_changes WHERE application_id=:id ORDER BY sequence DESC",
                Map.of("id", id));
    }

    public long nextSequence(UUID id) {
        return db.count(
                "SELECT coalesce(max(sequence),0)+1 FROM status_changes WHERE application_id=:id",
                Map.of("id", id));
    }

    public void insert(Map<String, Object> values) {
        db.insert("status_changes", values);
    }

    public UUID latestRejectionId(UUID id) {
        return (UUID)
                db.one(
                                "SELECT id FROM status_changes WHERE application_id=:id AND"
                                        + " new_status='REJECTED' ORDER BY sequence DESC LIMIT 1",
                                Map.of("id", id))
                        .get("id");
    }

    public void requireRejectionEvent(UUID id, Object event) {
        db.one(
                "SELECT id FROM status_changes WHERE id=:event AND application_id=:id AND"
                        + " new_status='REJECTED'",
                Map.of("event", event, "id", id));
    }
}
