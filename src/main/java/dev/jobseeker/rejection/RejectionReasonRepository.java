package dev.jobseeker.rejection;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RejectionReasonRepository {
    private final JdbcDatabase db;

    public RejectionReasonRepository(JdbcDatabase db) {
        this.db = db;
    }

    public List<Map<String, Object>> findByApplication(UUID id) {
        return db.rows(
                "SELECT r.*,t.name AS type_name,t.code AS type_code, EXISTS(SELECT 1 FROM"
                        + " rejection_reasons newer WHERE newer.supersedes_id=r.id) AS superseded FROM"
                        + " rejection_reasons r JOIN rejection_reason_types t ON t.id=r.type_id WHERE"
                        + " r.application_id=:id ORDER BY r.recorded_at DESC,r.id",
                Map.of("id", id));
    }

    public boolean isSuperseded(Object id) {
        return db.count(
                        "SELECT count(*) FROM rejection_reasons WHERE supersedes_id=:old",
                        Map.of("old", id))
                > 0;
    }

    public void insert(Map<String, Object> values) {
        db.insert("rejection_reasons", values);
    }
}
