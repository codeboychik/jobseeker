package dev.jobseeker.cv;

import java.util.Arrays;
import java.util.List;

public enum CvUsage {
    PREPARED,
    SENT;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
