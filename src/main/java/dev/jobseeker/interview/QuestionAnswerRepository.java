package dev.jobseeker.interview;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionAnswerRepository {
    private final JdbcDatabase db;

    public QuestionAnswerRepository(JdbcDatabase db) {
        this.db = db;
    }

    public List<Map<String, Object>> findByApplication(UUID id) {
        return db.rows(
                "SELECT q.* FROM questions q JOIN interview_rounds r ON r.id=q.round_id WHERE"
                        + " r.application_id=:id ORDER BY q.display_order,q.created_at,q.id",
                Map.of("id", id));
    }

    public List<Map<String, Object>> findByRound(UUID id) {
        return db.rows(
                "SELECT * FROM questions WHERE round_id=:id ORDER BY display_order,created_at,id",
                Map.of("id", id));
    }

    public Map<String, Object> findInRound(UUID id, UUID round) {
        return db.one(
                "SELECT * FROM questions WHERE id=:id AND round_id=:round",
                Map.of("id", id, "round", round));
    }

    public void insert(Map<String, Object> values) {
        db.insert("questions", values);
    }

    public void update(UUID id, Map<String, Object> values) {
        db.updateVersioned("questions", id, values);
    }

    public void delete(UUID id) {
        db.execute("DELETE FROM questions WHERE id=:id", Map.of("id", id));
    }
}
