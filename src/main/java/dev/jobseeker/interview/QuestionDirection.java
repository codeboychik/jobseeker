package dev.jobseeker.interview;

import java.util.Arrays;
import java.util.List;

public enum QuestionDirection {
    EMPLOYER_ASKED,
    CANDIDATE_ASKED;

    public static List<String> codes() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }
}
