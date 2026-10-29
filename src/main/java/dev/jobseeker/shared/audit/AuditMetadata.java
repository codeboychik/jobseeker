package dev.jobseeker.shared.audit;

import dev.jobseeker.shared.validation.InputValidator;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.Map;
import java.util.UUID;

public final class AuditMetadata {
    private AuditMetadata() {}

    public static Object actor(Map<String, Object> input) {
        return InputValidator.parse(
                        input,
                        ValidationRules.schema("actor", ValidationRules.text(false, 300)),
                        false)
                .get("actor");
    }

    public static Map<String, Object> create(
            Map<String, Object> values, Map<String, Object> input, InputChannel channel) {
        values.put("id", UUID.randomUUID());
        return update(values, input, channel);
    }

    public static Map<String, Object> update(
            Map<String, Object> values, Map<String, Object> input, InputChannel channel) {
        values.put("source", channel.name());
        values.put("actor", actor(input));
        return values;
    }
}
