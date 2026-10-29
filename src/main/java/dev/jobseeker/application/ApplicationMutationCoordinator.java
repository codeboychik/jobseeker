package dev.jobseeker.application;

import dev.jobseeker.search.SearchIndexService;
import dev.jobseeker.shared.audit.AuditMetadata;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.shared.error.ApplicationException;
import dev.jobseeker.shared.validation.InputValidator;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ApplicationMutationCoordinator {
    private final ApplicationRepository repository;
    private final SearchIndexService index;

    public ApplicationMutationCoordinator(
            ApplicationRepository repository, SearchIndexService index) {
        this.repository = repository;
        this.index = index;
    }

    public Map<String, Object> lock(UUID id, Map<String, Object> input, String versionKey) {
        var current = repository.lock(id);
        long expected = InputValidator.version(input, versionKey);
        if (((Number) current.get("version")).longValue() != expected)
            throw new ApplicationException(
                    409,
                    "This application changed. Review the current values and submit again.",
                    Map.of(versionKey, "The saved version is newer."),
                    current);
        return current;
    }

    public void touch(UUID id, Map<String, Object> input, InputChannel channel) {
        repository.touch(id, channel, AuditMetadata.actor(input));
        index.refresh(id);
    }
}
