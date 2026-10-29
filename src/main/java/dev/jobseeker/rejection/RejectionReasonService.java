package dev.jobseeker.rejection;

import dev.jobseeker.application.ApplicationMutationCoordinator;
import dev.jobseeker.application.ApplicationReferenceRepository;
import dev.jobseeker.application.ApplicationRepository;
import dev.jobseeker.application.StatusHistoryRepository;
import dev.jobseeker.shared.audit.AuditMetadata;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.shared.error.ApplicationException;
import dev.jobseeker.shared.validation.InputValidator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RejectionReasonService {
    private final RejectionReasonRepository repository;
    private final ApplicationRepository applications;
    private final StatusHistoryRepository history;
    private final ApplicationMutationCoordinator mutations;
    private final ApplicationReferenceRepository references;

    public RejectionReasonService(
            RejectionReasonRepository repository,
            ApplicationRepository applications,
            StatusHistoryRepository history,
            ApplicationMutationCoordinator mutations,
            ApplicationReferenceRepository references) {
        this.repository = repository;
        this.applications = applications;
        this.history = history;
        this.mutations = mutations;
        this.references = references;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID id) {
        applications.requireExists(id);
        return repository.findByApplication(id);
    }

    public UUID create(UUID id, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, RejectionReasonFields.FIELDS, "applicationVersion", "actor");
        var values = InputValidator.parse(input, RejectionReasonFields.FIELDS, true);
        var app = mutations.lock(id, input, "applicationVersion");
        references.reasonType(values.get("type_id"));
        references.round(id, values.get("round_id"));
        references.note(id, values.get("note_id"));
        references.reason(id, values.get("supersedes_id"));
        references.question(id, values.get("question_id"));
        if (!input.containsKey("rejectionEventId") && "REJECTED".equals(app.get("status")))
            values.put("rejection_event_id", history.latestRejectionId(id));
        if (values.get("rejection_event_id") != null)
            history.requireRejectionEvent(id, values.get("rejection_event_id"));
        if (values.get("supersedes_id") != null
                && repository.isSuperseded(values.get("supersedes_id")))
            throw new ApplicationException(
                    409,
                    "That assessment was already superseded. Correct the latest assessment"
                            + " instead.");
        UUID reason = UUID.randomUUID();
        values.put("id", reason);
        values.put("application_id", id);
        values.put("input_channel", channel.name());
        values.put("actor", AuditMetadata.actor(input));
        repository.insert(values);
        mutations.touch(id, input, channel);
        return reason;
    }
}
