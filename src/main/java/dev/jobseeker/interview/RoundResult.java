package dev.jobseeker.interview;

import java.util.Arrays;
import java.util.List;

public enum RoundResult {
    PENDING,
    PASSED,
    FAILED,
    NOT_PROVIDED;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
