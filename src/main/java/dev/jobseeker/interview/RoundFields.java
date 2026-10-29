package dev.jobseeker.interview;

import dev.jobseeker.shared.validation.FieldRule;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.Map;

public final class RoundFields {
    private RoundFields() {}

    public static final Map<String, FieldRule> FIELDS =
            ValidationRules.schema(
                    "typeId",
                    new FieldRule(true, ValidationRules.UUID_RULE.parse()),
                    "title",
                    ValidationRules.text(false, 500),
                    "displayOrder",
                    ValidationRules.INTEGER,
                    "scheduledAt",
                    ValidationRules.TIME,
                    "completedAt",
                    ValidationRules.TIME,
                    "status",
                    ValidationRules.choice(false, RoundStatus.codes()),
                    "result",
                    ValidationRules.choice(false, RoundResult.codes()),
                    "feedback",
                    ValidationRules.text(false, 200000));
}
