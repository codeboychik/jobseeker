package dev.jobseeker.rejection;

import java.util.Arrays;
import java.util.List;

public enum ReasonSource {
    COMPANY_FEEDBACK,
    SELF_ASSESSMENT,
    AI_ASSESSMENT;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
