package dev.jobseeker.idempotency;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepository {
    private final JdbcDatabase db;

    public IdempotencyRepository(JdbcDatabase db) {
        this.db = db;
    }

    public void lock(String scope) {
        db.rows("SELECT pg_advisory_xact_lock(hashtextextended(:scope,0))", Map.of("scope", scope));
    }

    public List<Map<String, Object>> find(String scope) {
        return db.rows(
                "SELECT * FROM idempotency_records WHERE scope=:scope", Map.of("scope", scope));
    }

    public void insert(Map<String, Object> values) {
        db.insert("idempotency_records", values);
    }
}
