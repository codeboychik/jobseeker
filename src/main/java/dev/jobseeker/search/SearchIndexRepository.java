package dev.jobseeker.search;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import dev.jobseeker.infrastructure.json.JsonCodec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SearchIndexRepository {
    private final JdbcDatabase db;
    private final JsonCodec json;

    public SearchIndexRepository(JdbcDatabase db, JsonCodec json) {
        this.db = db;
        this.json = json;
    }

    public Map<String, Object> application(UUID id) {
        return db.one("SELECT * FROM applications WHERE id=:id", Map.of("id", id));
    }

    public List<Map<String, Object>> history(UUID id) {
        return db.rows(
                "SELECT * FROM status_changes WHERE application_id=:id ORDER BY sequence",
                Map.of("id", id));
    }

    public List<Map<String, Object>> attachments(UUID id) {
        return db.rows(
                "SELECT c.*, f.filename,f.media_type,f.size,f.checksum,f.uploaded_at FROM"
                        + " cv_attachments c JOIN stored_files f ON f.id=c.file_id WHERE"
                        + " c.application_id=:id ORDER BY c.created_at,c.id",
                Map.of("id", id));
    }

    public List<Map<String, Object>> rounds(UUID id) {
        return db.rows(
                "SELECT r.*,t.name AS type_name FROM interview_rounds r JOIN round_types t ON"
                        + " t.id=r.type_id WHERE r.application_id=:id ORDER BY r.display_order,r.id",
                Map.of("id", id));
    }

    public List<Map<String, Object>> questions(UUID id) {
        return db.rows(
                "SELECT q.*,t.name AS type_name FROM questions q JOIN interview_rounds r ON"
                        + " r.id=q.round_id JOIN round_types t ON t.id=r.type_id WHERE"
                        + " r.application_id=:id ORDER BY r.display_order,r.id,q.display_order,q.id",
                Map.of("id", id));
    }

    public List<Map<String, Object>> notes(UUID id) {
        return db.rows(
                "SELECT * FROM notes WHERE application_id=:id ORDER BY created_at,id",
                Map.of("id", id));
    }

    public List<Map<String, Object>> reasons(UUID id) {
        return db.rows(
                "SELECT r.*,t.name AS category,t.code AS category_code FROM rejection_reasons r"
                        + " JOIN rejection_reason_types t ON t.id=r.type_id WHERE r.application_id=:id"
                        + " ORDER BY r.recorded_at,r.id",
                Map.of("id", id));
    }

    public void save(SearchDocument document) {
        db.execute(
                "INSERT INTO"
                        + " application_search(application_id,search_document,sections,indexed_version)"
                        + " VALUES (:id,:document,cast(:sections AS jsonb),:version) ON"
                        + " CONFLICT(application_id) DO UPDATE SET"
                        + " search_document=excluded.search_document,"
                        + " sections=excluded.sections,indexed_version=excluded.indexed_version",
                Map.of(
                        "id",
                        document.applicationId(),
                        "document",
                        document.text(),
                        "sections",
                        json.write(document.sections()),
                        "version",
                        document.version()));
    }

    public List<UUID> lockApplicationIds() {
        return db.rows("SELECT id FROM applications ORDER BY id FOR UPDATE", Map.of()).stream()
                .map(row -> (UUID) row.get("id"))
                .toList();
    }
}
