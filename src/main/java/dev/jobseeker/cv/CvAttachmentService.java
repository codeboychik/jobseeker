package dev.jobseeker.cv;

import dev.jobseeker.application.ApplicationMutationCoordinator;
import dev.jobseeker.application.ApplicationRepository;
import dev.jobseeker.shared.audit.AuditMetadata;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.shared.validation.InputValidator;
import dev.jobseeker.shared.validation.VersionCheck;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CvAttachmentService {
    private final CvAttachmentRepository repository;
    private final StoredFileRepository files;
    private final ApplicationRepository applications;
    private final ApplicationMutationCoordinator mutations;

    public CvAttachmentService(
            CvAttachmentRepository repository,
            StoredFileRepository files,
            ApplicationRepository applications,
            ApplicationMutationCoordinator mutations) {
        this.repository = repository;
        this.files = files;
        this.applications = applications;
        this.mutations = mutations;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID id) {
        applications.requireExists(id);
        var rows = repository.findByApplication(id);
        rows.forEach(
                row ->
                        row.put(
                                "downloadUrl",
                                "/api/v1/applications/"
                                        + id
                                        + "/cv-attachments/"
                                        + row.get("id")
                                        + "/download"));
        return rows;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> download(UUID app, UUID cv) {
        return files.download(app, cv);
    }

    public UUID upload(UUID id, ValidatedCv file, Map<String, Object> input, InputChannel channel) {
        var schema = CvAttachmentFields.UPLOAD;
        InputValidator.keys(input, schema, "applicationVersion", "actor");
        var values =
                AuditMetadata.create(InputValidator.parse(input, schema, false), input, channel);
        mutations.lock(id, input, "applicationVersion");
        UUID fileId = UUID.randomUUID();
        files.insert(file, fileId);
        values.put("application_id", id);
        values.put("file_id", fileId);
        values.computeIfAbsent("usage", k -> "PREPARED");
        repository.insert(values);
        mutations.touch(id, input, channel);
        return (UUID) values.get("id");
    }

    public void update(UUID id, UUID cv, Map<String, Object> input, InputChannel channel) {
        var schema = CvAttachmentFields.UPDATE;
        InputValidator.keys(input, schema, "applicationVersion", "version", "actor");
        mutations.lock(id, input, "applicationVersion");
        VersionCheck.child(repository.findOwned(cv, id), input);
        repository.update(
                cv,
                AuditMetadata.update(InputValidator.parse(input, schema, false), input, channel));
        mutations.touch(id, input, channel);
    }
}
