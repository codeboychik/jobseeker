package dev.jobseeker.shared.text;

import java.util.Locale;

public final class DisplayLabels {
    private DisplayLabels() {}

    public static String label(Object value) {
        if (value == null) return "Unknown";
        if ("OFFERED".equals(value)) return "Offer received";
        if ("ACCEPTED".equals(value)) return "Offer accepted";
        String s = value.toString().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
