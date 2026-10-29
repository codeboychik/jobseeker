package dev.jobseeker.statistics;

import dev.jobseeker.application.ApplicationStatus;
import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import dev.jobseeker.search.ApplicationFilter;
import dev.jobseeker.search.ApplicationSearchSql;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsRepository {
    private static final String COHORT =
            """
            WITH filtered AS (
                SELECT a.* %s %s
            ), current_rejections AS (
                SELECT f.id AS application_id, e.id AS event_id
                FROM filtered f
                JOIN LATERAL (
                    SELECT id FROM status_changes
                    WHERE application_id = f.id ORDER BY sequence DESC LIMIT 1
                ) e ON f.status = 'REJECTED'
            ), current_reasons AS (
                SELECT r.* FROM rejection_reasons r
                JOIN current_rejections c
                  ON c.event_id = r.rejection_event_id AND c.application_id = r.application_id
                WHERE NOT EXISTS (
                    SELECT 1 FROM rejection_reasons n WHERE n.supersedes_id = r.id
                )
            )
            """
                    .formatted(ApplicationSearchSql.FROM, ApplicationSearchSql.WHERE);
    private static final String COUNTS =
            """
            SELECT
                (SELECT count(*) FROM filtered) AS applications,
                (SELECT count(*) FROM current_rejections) AS currently_rejected,
                (SELECT count(*) FROM filtered f WHERE EXISTS (
                    SELECT 1 FROM status_changes s
                    WHERE s.application_id = f.id AND s.new_status = 'REJECTED'
                )) AS ever_rejected,
                (SELECT count(*) FROM status_changes s JOIN filtered f ON f.id = s.application_id
                    WHERE s.new_status = 'REJECTED') AS rejection_events,
                (SELECT count(*) FROM current_reasons) AS reason_entries,
                (SELECT count(*) FROM current_rejections c WHERE NOT EXISTS (
                    SELECT 1 FROM current_reasons r WHERE r.application_id = c.application_id
                )) AS rejections_without_reason
            """;
    private static final String REASONS_BY_SOURCE =
            """
            SELECT t.id AS type_id, t.code, t.name, r.source,
                count(*) AS entry_count, count(DISTINCT r.application_id) AS application_count
            FROM current_reasons r JOIN rejection_reason_types t ON t.id = r.type_id
            GROUP BY t.id, t.code, t.name, r.source
            ORDER BY application_count DESC, t.name, r.source
            """;
    private static final String REASONS_BY_CATEGORY =
            """
            SELECT t.id AS type_id, t.code, t.name,
                count(*) AS entry_count, count(DISTINCT r.application_id) AS application_count
            FROM current_reasons r JOIN rejection_reason_types t ON t.id = r.type_id
            GROUP BY t.id, t.code, t.name ORDER BY application_count DESC, t.name
            """;

    private final JdbcDatabase db;

    public StatisticsRepository(JdbcDatabase db) {
        this.db = db;
    }

    public Map<String, Object> summary(ApplicationFilter filter) {
        var parameters = ApplicationSearchSql.parameters(filter);
        var result = db.one(COHORT + COUNTS, parameters);
        result.put("statusCounts", statusCounts(parameters));
        result.put("reasonGroups", db.rows(COHORT + REASONS_BY_SOURCE, parameters));
        result.put("reasonCategories", db.rows(COHORT + REASONS_BY_CATEGORY, parameters));
        return result;
    }

    private Map<String, Long> statusCounts(Map<String, Object> parameters) {
        var counts = new LinkedHashMap<String, Long>();
        ApplicationStatus.codes().forEach(status -> counts.put(status, 0L));
        db.rows(
                        COHORT + "SELECT status, count(*) AS count FROM filtered GROUP BY status",
                        parameters)
                .forEach(
                        row ->
                                counts.put(
                                        row.get("status").toString(),
                                        ((Number) row.get("count")).longValue()));
        return counts;
    }
}
