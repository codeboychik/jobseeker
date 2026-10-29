package dev.jobseeker.application;

import dev.jobseeker.shared.validation.FieldRule;
import dev.jobseeker.shared.validation.ValidationRules;
import java.util.Map;

public final class ApplicationFields {
    private ApplicationFields() {}

    public static final Map<String, FieldRule> INITIAL_STATUS =
            ValidationRules.schema(
                    "status", ValidationRules.choice(false, ApplicationStatus.codes()));
    public static final Map<String, FieldRule> STATUS_EVENT =
            ValidationRules.schema(
                    "eventTime",
                    ValidationRules.TIME,
                    "explanation",
                    ValidationRules.text(false, 200000));
    public static final Map<String, FieldRule> STATUS_CHANGE =
            ValidationRules.schema(
                    "status", ValidationRules.choice(true, ApplicationStatus.codes()),
                    "eventTime", ValidationRules.TIME,
                    "explanation", ValidationRules.text(false, 200000));
    public static final Map<String, FieldRule> ARCHIVE =
            ValidationRules.schema("archived", ValidationRules.BOOLEAN);

    public static final Map<String, FieldRule> FIELDS =
            ValidationRules.schema(
                    "company",
                    ValidationRules.text(true, 300),
                    "role",
                    ValidationRules.text(true, 300),
                    "jobUrl",
                    ValidationRules.URL,
                    "externalReference",
                    ValidationRules.text(false, 500),
                    "postedDate",
                    ValidationRules.DATE,
                    "appliedDate",
                    ValidationRules.DATE,
                    "description",
                    ValidationRules.text(false, 200000),
                    "companyFeedback",
                    ValidationRules.text(false, 200000),
                    "expectedAmount",
                    ValidationRules.MONEY,
                    "expectedCurrency",
                    ValidationRules.text(false, 3),
                    "expectedPeriod",
                    ValidationRules.choice(false, SalaryPeriod.codes()),
                    "expectedBasis",
                    ValidationRules.choice(false, SalaryBasis.codes()),
                    "offeredAmount",
                    ValidationRules.MONEY,
                    "offeredCurrency",
                    ValidationRules.text(false, 3),
                    "offeredPeriod",
                    ValidationRules.choice(false, SalaryPeriod.codes()),
                    "offeredBasis",
                    ValidationRules.choice(false, SalaryBasis.codes()));
}
