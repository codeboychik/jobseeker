package dev.jobseeker.shared.validation;

import dev.jobseeker.shared.error.ApplicationException;
import java.util.Map;

public final class VersionCheck {
    private VersionCheck() {}

    public static void child(Map<String, Object> row, Map<String, Object> input) {
        if (((Number) row.get("version")).longValue() != InputValidator.version(input, "version"))
            throw new ApplicationException(
                    409,
                    "This record changed. Review current values before retrying.",
                    Map.of(),
                    row);
    }

    public static void requiredIfPresent(Map<String, Object> values, String... fields) {
        for (String field : fields)
            if (values.containsKey(field) && values.get(field) == null)
                throw ApplicationException.field(field, "This field cannot be cleared.");
    }
}
