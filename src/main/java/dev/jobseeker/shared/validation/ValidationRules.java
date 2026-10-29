package dev.jobseeker.shared.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ValidationRules {
    private ValidationRules() {}

    public static FieldRule text(boolean required, int max) {
        return new FieldRule(
                required,
                v -> {
                    if (!(v instanceof String s) || s.length() > max)
                        throw new IllegalArgumentException(
                                "Enter text of at most " + max + " characters.");
                    if (s.isBlank()) return null;
                    return max <= 500 ? s.strip() : s;
                });
    }

    public static FieldRule choice(boolean required, Collection<String> options) {
        return new FieldRule(
                required,
                v -> {
                    String s = v.toString();
                    if (!options.contains(s))
                        throw new IllegalArgumentException(
                                "Choose one of: " + String.join(", ", options));
                    return s;
                });
    }

    public static final FieldRule UUID_RULE =
            new FieldRule(false, v -> UUID.fromString(v.toString()));
    public static final FieldRule DATE = new FieldRule(false, v -> LocalDate.parse(v.toString()));
    public static final FieldRule TIME =
            new FieldRule(false, v -> OffsetDateTime.parse(v.toString()).toInstant());
    public static final FieldRule INTEGER =
            new FieldRule(
                    false,
                    v -> {
                        int n = new BigDecimal(v.toString()).intValueExact();
                        if (n < 0)
                            throw new IllegalArgumentException("Use zero or a positive integer.");
                        return n;
                    });
    public static final FieldRule MONEY =
            new FieldRule(
                    false,
                    v -> {
                        BigDecimal n =
                                new BigDecimal(v.toString()).setScale(2, RoundingMode.UNNECESSARY);
                        if (n.signum() <= 0 || n.precision() > 16)
                            throw new IllegalArgumentException(
                                    "Enter a positive amount with at most two decimal places.");
                        return n;
                    });
    public static final FieldRule LIKELIHOOD =
            new FieldRule(
                    false,
                    v -> {
                        BigDecimal n =
                                new BigDecimal(v.toString()).setScale(2, RoundingMode.UNNECESSARY);
                        if (n.signum() < 0 || n.compareTo(BigDecimal.valueOf(100)) > 0)
                            throw new IllegalArgumentException("Use a value from 0 to 100.");
                        return n;
                    });
    public static final FieldRule BOOLEAN =
            new FieldRule(
                    true,
                    v -> {
                        if (v instanceof Boolean) return v;
                        if ("true".equals(v) || "false".equals(v))
                            return Boolean.valueOf(v.toString());
                        throw new IllegalArgumentException("Use true or false.");
                    });
    public static final FieldRule URL =
            new FieldRule(
                    false,
                    v -> {
                        String s = (String) text(false, 4000).parse().apply(v);
                        if (s == null) return null;
                        URI uri = URI.create(s);
                        if (!List.of("http", "https").contains(uri.getScheme())
                                || uri.getHost() == null)
                            throw new IllegalArgumentException(
                                    "Use an absolute http or https URL.");
                        return s;
                    });

    public static Map<String, FieldRule> schema(Object... entries) {
        var result = new LinkedHashMap<String, FieldRule>();
        for (int i = 0; i < entries.length; i += 2)
            result.put((String) entries[i], (FieldRule) entries[i + 1]);
        return Collections.unmodifiableMap(result);
    }
}
