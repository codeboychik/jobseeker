package dev.jobseeker.application;

import dev.jobseeker.search.SearchIndexService;
import dev.jobseeker.shared.audit.AuditMetadata;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.shared.text.FieldNames;
import dev.jobseeker.shared.validation.InputValidator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApplicationService {
    private final ApplicationRepository repository;
    private final StatusHistoryRepository history;
    private final ApplicationMutationCoordinator mutations;
    private final SearchIndexService index;

    public ApplicationService(
            ApplicationRepository repository,
            StatusHistoryRepository history,
            ApplicationMutationCoordinator mutations,
            SearchIndexService index) {
        this.repository = repository;
        this.history = history;
        this.mutations = mutations;
        this.index = index;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(UUID id) {
        repository.requireExists(id);
        return history.findByApplication(id);
    }

    public UUID create(Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(
                input, ApplicationFields.FIELDS, "status", "eventTime", "explanation", "actor");
        var values =
                AuditMetadata.create(
                        InputValidator.parse(input, ApplicationFields.FIELDS, true),
                        input,
                        channel);
        SalaryValidator.validate(values);
        String status =
                (String)
                        InputValidator.parse(input, ApplicationFields.INITIAL_STATUS, false)
                                .getOrDefault("status", "SAVED");
        if (status == null) status = "SAVED";
        values.put("status", status);
        UUID id = (UUID) values.get("id");
        repository.insert(values);
        statusEvent(id, null, status, input, channel);
        index.refresh(id);
        return id;
    }

    public void update(UUID id, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, ApplicationFields.FIELDS, "version", "actor");
        var current = mutations.lock(id, input, "version");
        var values = InputValidator.parse(input, ApplicationFields.FIELDS, false);
        var combined = new HashMap<String, Object>();
        ApplicationFields.FIELDS
                .keySet()
                .forEach(key -> combined.put(FieldNames.toColumnName(key), current.get(key)));
        combined.putAll(values);
        SalaryValidator.validate(combined);
        // Currency is normalized even when only another salary field changes.
        for (String prefix : List.of("expected", "offered"))
            if (combined.get(prefix + "_currency") != null)
                values.put(prefix + "_currency", combined.get(prefix + "_currency"));
        repository.update(id, values);
        mutations.touch(id, input, channel);
    }

    public void status(UUID id, Map<String, Object> input, InputChannel channel) {
        InputValidator.keys(input, ApplicationFields.STATUS_CHANGE, "version", "actor");
        var parsed = InputValidator.parse(input, ApplicationFields.STATUS_CHANGE, true);
        var current = mutations.lock(id, input, "version");
        String status = (String) parsed.get("status");
        if (status.equals(current.get("status"))) return;
        repository.update(id, new HashMap<>(Map.of("status", status)));
        statusEvent(id, current.get("status"), status, input, channel);
        mutations.touch(id, input, channel);
    }

    private void statusEvent(
            UUID id,
            Object previous,
            String status,
            Map<String, Object> input,
            InputChannel channel) {
        var values =
                AuditMetadata.create(
                        InputValidator.parse(input, ApplicationFields.STATUS_EVENT, false),
                        input,
                        channel);
        values.put("application_id", id);
        values.put("previous_status", previous);
        values.put("new_status", status);
        values.put("sequence", history.nextSequence(id));
        history.insert(values);
    }

    public void archive(UUID id, Map<String, Object> input, InputChannel channel) {
        var schema = ApplicationFields.ARCHIVE;
        InputValidator.keys(input, schema, "version", "actor");
        var values = InputValidator.parse(input, schema, true);
        mutations.lock(id, input, "version");
        repository.update(id, values);
        mutations.touch(id, input, channel);
    }
}
