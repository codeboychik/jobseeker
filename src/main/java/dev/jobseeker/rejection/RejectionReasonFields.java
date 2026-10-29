package dev.jobseeker.rejection;

import dev.jobseeker.shared.validation.FieldRule;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.Map;

public final class RejectionReasonFields {
    private RejectionReasonFields() {}

    public static final Map<String, FieldRule> FIELDS =
            ValidationRules.schema(
                    "typeId",
                    new FieldRule(true, ValidationRules.UUID_RULE.parse()),
                    "label",
                    ValidationRules.text(true, 500),
                    "explanation",
                    ValidationRules.text(false, 200000),
                    "source",
                    ValidationRules.choice(true, ReasonSource.codes()),
                    "likelihood",
                    ValidationRules.LIKELIHOOD,
                    "evidence",
                    ValidationRules.text(false, 200000),
                    "roundId",
                    ValidationRules.UUID_RULE,
                    "questionId",
                    ValidationRules.UUID_RULE,
                    "noteId",
                    ValidationRules.UUID_RULE,
                    "rejectionEventId",
                    ValidationRules.UUID_RULE,
                    "assessedAt",
                    ValidationRules.TIME,
                    "model",
                    ValidationRules.text(false, 300),
                    "supersedesId",
                    ValidationRules.UUID_RULE);
}
