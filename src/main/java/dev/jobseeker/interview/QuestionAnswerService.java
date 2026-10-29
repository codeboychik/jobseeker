package dev.jobseeker.interview;

import dev.jobseeker.application.ApplicationMutationCoordinator;
import dev.jobseeker.application.ApplicationReferenceRepository;
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
public class QuestionAnswerService {
    private final QuestionAnswerRepository repository;
    private final ApplicationMutationCoordinator mutations;
    private final ApplicationReferenceRepository references;

    public QuestionAnswerService(
            QuestionAnswerRepository repository,
            ApplicationMutationCoordinator mutations,
            ApplicationReferenceRepository references) {
        this.repository = repository;
        this.mutations = mutations;
        this.references = references;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(UUID id, UUID round) {
        references.round(id, round);
        return repository.findByRound(round);
    }

    public UUID create(UUID id, UUID round, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, QuestionFields.FIELDS, "applicationVersion", "actor");
        var values =
                AuditMetadata.create(
                        InputValidator.parse(input, QuestionFields.FIELDS, true), input, channel);
        mutations.lock(id, input, "applicationVersion");
        references.round(id, round);
        references.note(id, values.get("source_note_id"));
        values.put("round_id", round);
        values.computeIfAbsent("display_order", k -> 0);
        repository.insert(values);
        mutations.touch(id, input, channel);
        return (UUID) values.get("id");
    }

    public void update(
            UUID id, UUID round, UUID question, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, QuestionFields.FIELDS, "applicationVersion", "version", "actor");
        mutations.lock(id, input, "applicationVersion");
        references.round(id, round);
        VersionCheck.child(repository.findInRound(question, round), input);
        var values = InputValidator.parse(input, QuestionFields.FIELDS, false);
        VersionCheck.requiredIfPresent(values, "display_order");
        references.note(id, values.get("source_note_id"));
        repository.update(question, AuditMetadata.update(values, input, channel));
        mutations.touch(id, input, channel);
    }

    public void delete(
            UUID id, UUID round, UUID question, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, Map.of(), "applicationVersion", "version", "actor");
        mutations.lock(id, input, "applicationVersion");
        references.round(id, round);
        VersionCheck.child(repository.findInRound(question, round), input);
        repository.delete(question);
        mutations.touch(id, input, channel);
    }
}
