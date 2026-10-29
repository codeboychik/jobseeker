package dev.jobseeker.application;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import dev.jobseeker.shared.audit.InputChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationRepository {
    private final JdbcDatabase db;

    public ApplicationRepository(JdbcDatabase db) {
        this.db = db;
    }

    public Map<String, Object> find(UUID id) {
        return db.one("SELECT * FROM applications WHERE id=:id", Map.of("id", id));
    }

    public void requireExists(UUID id) {
        db.one("SELECT id FROM applications WHERE id=:id", Map.of("id", id));
    }

    public Map<String, Object> lock(UUID id) {
        return db.one("SELECT * FROM applications WHERE id=:id FOR UPDATE", Map.of("id", id));
    }

    public void insert(Map<String, Object> values) {
        db.insert("applications", values);
    }

    public void update(UUID id, Map<String, Object> values) {
        db.update("applications", id, values);
    }

    public void touch(UUID id, InputChannel channel, Object actor) {
        var params = new HashMap<String, Object>();
        params.put("id", id);
        params.put("source", channel.name());
        params.put("actor", actor);
        db.execute(
                "UPDATE applications SET"
                        + " version=version+1,updated_at=clock_timestamp(),source=:source,actor=:actor"
                        + " WHERE id=:id",
                params);
    }

    public long duplicateCount(Map<String, Object> app) {
        return db.count(
                "SELECT count(*) FROM applications WHERE id<>:id AND ((:url<>'' AND job_url=:url)"
                        + " OR (:ref<>'' AND external_reference=:ref))",
                Map.of(
                        "id",
                        app.get("id"),
                        "url",
                        Objects.toString(app.get("jobUrl"), ""),
                        "ref",
                        Objects.toString(app.get("externalReference"), "")));
    }
}
