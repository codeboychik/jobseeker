package dev.jobseeker.shared.audit;

import java.util.Arrays;
import java.util.List;

public enum InputChannel {
    MANUAL,
    AUTOMATION;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
