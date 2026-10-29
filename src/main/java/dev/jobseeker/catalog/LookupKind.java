package dev.jobseeker.catalog;

public enum LookupKind {
    ROUND("round_types"),
    REJECTION_REASON("rejection_reason_types");
    private final String table;

    LookupKind(String table) {
        this.table = table;
    }

    public String table() {
        return table;
    }
}
