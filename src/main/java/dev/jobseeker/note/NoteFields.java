package dev.jobseeker.note;

import dev.jobseeker.shared.validation.FieldRule;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.Map;

public final class NoteFields {
    private NoteFields() {}

    public static final Map<String, FieldRule> FIELDS =
            ValidationRules.schema(
                    "text",
                    ValidationRules.text(true, 200000),
                    "roundId",
                    ValidationRules.UUID_RULE);
}
