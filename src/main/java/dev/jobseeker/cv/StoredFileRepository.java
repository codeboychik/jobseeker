package dev.jobseeker.cv;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class StoredFileRepository {
    private final JdbcDatabase db;

    public StoredFileRepository(JdbcDatabase db) {
        this.db = db;
    }

    public void insert(ValidatedCv file, UUID id) {
        db.insert(
                "stored_files",
                new HashMap<>(
                        Map.of("id", id,
                            "content", file.bytes(),
                            "filename", file.filename(),
                            "media_type", file.mediaType(),
                            "size", file.size(),
                            "checksum", file.checksum())));
}

    public Map<String, Object> download(UUID app, UUID cv) {
        return db.one(
                "SELECT f.filename,f.media_type,f.content,f.checksum FROM stored_files f JOIN"
                        + " cv_attachments c ON c.file_id=f.id WHERE c.application_id=:app AND"
                        + " c.id=:id",
                Map.of("app", app, "id", cv));
    }
}
