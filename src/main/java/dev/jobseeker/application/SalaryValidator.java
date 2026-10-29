package dev.jobseeker.application;

import dev.jobseeker.shared.error.ApplicationException;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SalaryValidator {
    private SalaryValidator() {}

    public static void validate(Map<String, Object> values) {
        for (String prefix : List.of("expected", "offered")) {
            Object currency = values.get(prefix + "_currency");
            if (currency != null) {
                String code = currency.toString().toUpperCase(Locale.ROOT);
                try {
                    Currency.getInstance(code);
                } catch (IllegalArgumentException e) {
                    throw ApplicationException.field(
                            prefix + "Currency", "Choose a valid three-letter currency code.");
                }
                values.put(prefix + "_currency", code);
            }
            if (values.get(prefix + "_amount") != null
                    && (currency == null || values.get(prefix + "_period") == null))
                throw ApplicationException.field(
                        prefix + "Amount", "Currency and period are required with an amount.");
        }
    }
}
