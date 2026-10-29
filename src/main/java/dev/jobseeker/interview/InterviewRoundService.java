package dev.jobseeker.interview;

import dev.jobseeker.application.ApplicationMutationCoordinator;
import dev.jobseeker.application.ApplicationReferenceRepository;
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
public class InterviewRoundService {
    private final InterviewRoundRepository repository;
    private final ApplicationRepository applications;
    private final ApplicationMutationCoordinator mutations;
    private final ApplicationReferenceRepository references;

    public InterviewRoundService(
            InterviewRoundRepository repository,
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
        InputValidator.keys(input, RoundFields.FIELDS, "applicationVersion", "actor");
        var values =
                AuditMetadata.create(
                        InputValidator.parse(input, RoundFields.FIELDS, true), input, channel);
        mutations.lock(id, input, "applicationVersion");
        references.roundType(values.get("type_id"));
        values.put("application_id", id);
        values.computeIfAbsent("status", k -> "PLANNED");
        values.computeIfAbsent("result", k -> "PENDING");
        values.computeIfAbsent("display_order", k -> 0);
        repository.insert(values);
        mutations.touch(id, input, channel);
        return (UUID) values.get("id");
    }

    public void update(UUID id, UUID round, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, RoundFields.FIELDS, "applicationVersion", "version", "actor");
        mutations.lock(id, input, "applicationVersion");
        var current = repository.findOwned(round, id);
        VersionCheck.child(current, input);
        var values = InputValidator.parse(input, RoundFields.FIELDS, false);
        if (values.containsKey("type_id")) references.roundType(values.get("type_id"));
        VersionCheck.requiredIfPresent(values, "status", "result", "display_order");
        repository.update(round, AuditMetadata.update(values, input, channel));
        mutations.touch(id, input, channel);
    }
}
