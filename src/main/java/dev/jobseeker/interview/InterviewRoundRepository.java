package dev.jobseeker.interview;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class InterviewRoundRepository {
    private final JdbcDatabase db;

    public InterviewRoundRepository(JdbcDatabase db) {
        this.db = db;
    }

    public List<Map<String, Object>> findByApplication(UUID id) {
        return db.rows(
                "SELECT r.*,t.name AS type_name FROM interview_rounds r JOIN round_types t ON"
                        + " t.id=r.type_id WHERE r.application_id=:id ORDER BY"
                        + " r.display_order,r.created_at,r.id",
                Map.of("id", id));
    }

    public Map<String, Object> findOwned(UUID id, UUID app) {
        return db.one(
                "SELECT * FROM interview_rounds WHERE id=:id AND application_id=:app",
                Map.of("id", id, "app", app));
    }

    public void insert(Map<String, Object> values) {
        db.insert("interview_rounds", values);
    }

    public void update(UUID id, Map<String, Object> values) {
        db.updateVersioned("interview_rounds", id, values);
    }
}
