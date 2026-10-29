package dev.jobseeker.interview;

import dev.jobseeker.shared.validation.FieldRule;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.Map;

public final class QuestionFields {
    private QuestionFields() {}

    public static final Map<String, FieldRule> FIELDS =
            ValidationRules.schema(
                    "direction",
                    ValidationRules.choice(true, QuestionDirection.codes()),
                    "question",
                    ValidationRules.text(true, 200000),
                    "answer",
                    ValidationRules.text(false, 200000),
                    "notes",
                    ValidationRules.text(false, 200000),
                    "displayOrder",
                    ValidationRules.INTEGER,
                    "sourceNoteId",
                    ValidationRules.UUID_RULE);
}
