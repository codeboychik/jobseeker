package dev.jobseeker.interview;

import java.util.Arrays;
import java.util.List;

public enum RoundStatus {
    PLANNED,
    COMPLETED,
    CANCELLED;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
