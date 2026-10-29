package dev.jobseeker.catalog;

import dev.jobseeker.shared.error.ApplicationException;
import dev.jobseeker.shared.validation.InputValidator;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LookupTypeService {
    private final LookupTypeRepository repository;

    public LookupTypeService(LookupTypeRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> list(LookupKind kind) {
        return repository.list(kind);
    }

    @Transactional
    public UUID create(LookupKind kind, Map<String, Object> input) {
        var schema = ValidationRules.schema("name", ValidationRules.text(true, 200));
        InputValidator.keys(input, schema);
        var values = InputValidator.parse(input, schema, true);
        UUID id = UUID.randomUUID();
        values.put("id", id);
        String normalized = values.get("name").toString().toLowerCase(Locale.ROOT);
        values.put("normalized_name", normalized);
        if (repository.nameExists(kind, normalized))
            throw ApplicationException.field("name", "This type already exists.");
        if (kind == LookupKind.REJECTION_REASON)
            values.put("code", "CUSTOM_" + id.toString().replace("-", "").toUpperCase(Locale.ROOT));
        repository.insert(kind, values);
        return id;
    }
}
