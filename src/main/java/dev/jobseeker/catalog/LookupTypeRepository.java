package dev.jobseeker.catalog;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class LookupTypeRepository {
    private final JdbcDatabase db;

    public LookupTypeRepository(JdbcDatabase db) {
        this.db = db;
    }

    public List<Map<String, Object>> list(LookupKind kind) {
        return db.rows(
                "SELECT * FROM " + kind.table() + " ORDER BY predefined DESC,name,id", Map.of());
    }

    public boolean nameExists(LookupKind kind, String name) {
        return db.count(
                        "SELECT count(*) FROM " + kind.table() + " WHERE normalized_name=:name",
                        Map.of("name", name))
                > 0;
    }

    public void insert(LookupKind kind, Map<String, Object> values) {
        db.insert(kind.table(), values);
    }
}
