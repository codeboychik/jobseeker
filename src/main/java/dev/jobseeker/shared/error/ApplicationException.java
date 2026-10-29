package dev.jobseeker.shared.error;

import java.util.Map;

public class ApplicationException extends RuntimeException {
    private final int status;
    private final Map<String, String> fields;
    private final Object current;

    public ApplicationException(int status, String message) {
        this(status, message, Map.of(), null);
    }

    public ApplicationException(
            int status, String message, Map<String, String> fields, Object current) {
        super(message);
        this.status = status;
        this.fields = fields;
        this.current = current;
    }

    public static ApplicationException field(String field, String message) {
        return new ApplicationException(
                400, "Please check the entered values.", Map.of(field, message), null);
    }

    public int status() {
        return status;
    }

    public Map<String, String> fields() {
        return fields;
    }

    public Object current() {
        return current;
    }
}
