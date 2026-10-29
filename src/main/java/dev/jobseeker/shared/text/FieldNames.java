package dev.jobseeker.shared.text;

import java.util.Locale;

public final class FieldNames {
    private FieldNames() {}

    public static String toColumnName(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    public static String toPropertyName(String value) {
        var result = new StringBuilder();
        boolean upper = false;
        for (char c : value.toCharArray()) {
            if (c == '_') upper = true;
            else {
                result.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return result.toString();
    }
}
