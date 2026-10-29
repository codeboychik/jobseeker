package dev.jobseeker.application;

import java.util.Arrays;
import java.util.List;

public enum ApplicationStatus {
    SAVED,
    APPLIED,
    INTERVIEWING,
    OFFERED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN,
    CLOSED;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
