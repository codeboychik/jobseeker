package dev.jobseeker.shared.validation;

import dev.jobseeker.shared.error.ApplicationException;
import dev.jobseeker.shared.text.FieldNames;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class InputValidator {
    private InputValidator() {}

    public static void keys(
            Map<String, Object> input, Map<String, FieldRule> schema, String... additional) {
        var allowed = new HashSet<>(schema.keySet());
        allowed.addAll(List.of(additional));
        var errors = new LinkedHashMap<String, String>();
        input.keySet().stream()
                .filter(k -> !allowed.contains(k))
                .forEach(k -> errors.put(k, "Unknown field."));
        if (!errors.isEmpty())
            throw new ApplicationException(400, "Unknown input fields.", errors, null);
    }

    public static Map<String, Object> parse(
            Map<String, Object> input, Map<String, FieldRule> schema, boolean create) {
        var values = new LinkedHashMap<String, Object>();
        var errors = new LinkedHashMap<String, String>();
        schema.forEach(
                (key, rule) -> {
                    if (!input.containsKey(key) && !(create && rule.required())) return;
                    Object raw = input.get(key), value = null;
                    try {
                        if (raw != null && !(raw instanceof String text && text.isBlank()))
                            value = rule.parse().apply(raw);
                        if (value == null && rule.required())
                            throw new IllegalArgumentException("This field is required.");
                        values.put(FieldNames.toColumnName(key), value);
                    } catch (RuntimeException exception) {
                        errors.put(
                                key,
                                exception instanceof IllegalArgumentException
                                                && exception.getMessage() != null
                                        ? exception.getMessage()
                                        : "Invalid value.");
                    }
                });
        if (!errors.isEmpty())
            throw new ApplicationException(400, "Please check the entered values.", errors, null);
        return values;
    }

    public static long version(Map<String, Object> input, String key) {
        try {
            long value = new BigDecimal(Objects.toString(input.get(key), "")).longValueExact();
            if (value < 0) throw new IllegalArgumentException();
            return value;
        } catch (RuntimeException e) {
            throw ApplicationException.field(key, "Supply the last-read version.");
        }
    }

    public static UUID uuid(Object value, String field) {
        try {
            return UUID.fromString(Objects.toString(value, ""));
        } catch (RuntimeException e) {
            throw ApplicationException.field(field, "Choose a valid reference.");
        }
    }
}
