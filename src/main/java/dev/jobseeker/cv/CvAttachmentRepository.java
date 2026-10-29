package dev.jobseeker.cv;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CvAttachmentRepository {
    private final JdbcDatabase db;

    public CvAttachmentRepository(JdbcDatabase db) {
        this.db = db;
    }

    public List<Map<String, Object>> findByApplication(UUID id) {
        return db.rows(
                "SELECT c.*, f.filename,f.media_type,f.size,f.checksum,f.uploaded_at FROM"
                        + " cv_attachments c JOIN stored_files f ON f.id=c.file_id WHERE"
                        + " c.application_id=:id ORDER BY c.created_at,c.id",
                Map.of("id", id));
    }

    public Map<String, Object> findOwned(UUID id, UUID app) {
        return db.one(
                "SELECT * FROM cv_attachments WHERE id=:id AND application_id=:app",
                Map.of("id", id, "app", app));
    }

    public void insert(Map<String, Object> values) {
        db.insert("cv_attachments", values);
    }

    public void update(UUID id, Map<String, Object> values) {
        db.updateVersioned("cv_attachments", id, values);
    }
}
