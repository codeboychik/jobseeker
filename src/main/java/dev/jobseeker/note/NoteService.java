package dev.jobseeker.note;

import dev.jobseeker.application.ApplicationMutationCoordinator;
import dev.jobseeker.application.ApplicationReferenceRepository;
import dev.jobseeker.application.ApplicationRepository;
import dev.jobseeker.shared.audit.AuditMetadata;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.shared.validation.InputValidator;
import dev.jobseeker.shared.validation.ValidationRules;
import dev.jobseeker.shared.validation.VersionCheck;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NoteService {
    private final NoteRepository repository;
    private final ApplicationRepository applications;
    private final ApplicationMutationCoordinator mutations;
    private final ApplicationReferenceRepository references;

    public NoteService(
            NoteRepository repository,
            ApplicationRepository applications,
            ApplicationMutationCoordinator mutations,
            ApplicationReferenceRepository references) {
        this.repository = repository;
        this.applications = applications;
        this.mutations = mutations;
        this.references = references;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID id) {
        applications.requireExists(id);
        return repository.findByApplication(id);
    }

    public UUID create(UUID id, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, NoteFields.FIELDS, "applicationVersion", "actor");
        var values =
                AuditMetadata.create(
                        InputValidator.parse(input, NoteFields.FIELDS, true), input, channel);
        mutations.lock(id, input, "applicationVersion");
        references.round(id, values.get("round_id"));
        values.put("application_id", id);
        repository.insert(values);
        mutations.touch(id, input, channel);
        return (UUID) values.get("id");
    }

    public void update(UUID id, UUID note, Map<String, Object> input, InputChannel channel) {
        var schema = ValidationRules.schema("text", ValidationRules.text(true, 200000));
        InputValidator.keys(input, schema, "applicationVersion", "version", "actor");
        mutations.lock(id, input, "applicationVersion");
        VersionCheck.child(repository.findOwned(note, id), input);
        repository.update(
                note,
                AuditMetadata.update(InputValidator.parse(input, schema, false), input, channel));
        mutations.touch(id, input, channel);
    }
}
