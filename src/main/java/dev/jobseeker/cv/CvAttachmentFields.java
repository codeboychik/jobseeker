package dev.jobseeker.cv;

import dev.jobseeker.shared.validation.FieldRule;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.Map;

public final class CvAttachmentFields {
    private CvAttachmentFields() {}

    public static final Map<String, FieldRule> UPLOAD =
            ValidationRules.schema(
                    "usage",
                    ValidationRules.choice(false, CvUsage.codes()),
                    "sentDate",
                    ValidationRules.DATE);
    public static final Map<String, FieldRule> UPDATE =
            ValidationRules.schema(
                    "usage",
                    ValidationRules.choice(true, CvUsage.codes()),
                    "sentDate",
                    ValidationRules.DATE);
}
