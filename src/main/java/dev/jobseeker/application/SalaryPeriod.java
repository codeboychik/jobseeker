package dev.jobseeker.application;

import java.util.Arrays;
import java.util.List;

public enum SalaryPeriod {
    HOUR,
    MONTH,
    YEAR;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
