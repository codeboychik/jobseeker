package dev.jobseeker.search;

import java.util.Locale;
import java.util.Map;

public final class ApplicationSearchSql {
    private ApplicationSearchSql() {}

    public static final String FROM =
            " FROM applications a LEFT JOIN application_search s ON s.application_id=a.id ";
    public static final String WHERE =
            " WHERE (:status='' OR a.status=:status) AND (:archive='all' OR"
                    + " a.archived=(:archive='archived')) AND (:q='' OR s.search_vector @@"
                    + " plainto_tsquery('simple',:q) OR position(lower(:q) IN"
                    + " lower(s.search_document))>0) ";

    public static Map<String, Object> parameters(ApplicationFilter f) {
        return Map.of(
                "q",
                f.q().toLowerCase(Locale.ROOT),
                "status",
                f.status(),
                "archive",
                f.archive(),
                "limit",
                f.size(),
                "offset",
                Math.multiplyExact((long) f.page(), f.size()));
    }
}
