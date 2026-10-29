package dev.jobseeker.web.mvc.support;

public final class ReturnLocation {
    private ReturnLocation() {}

    public static String sanitize(String value) {
        return value != null
                        && (value.equals("/applications") || value.startsWith("/applications?"))
                ? value
                : "/applications";
    }
}
