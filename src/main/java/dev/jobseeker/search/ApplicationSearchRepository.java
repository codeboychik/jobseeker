package dev.jobseeker.search;

import dev.jobseeker.infrastructure.jdbc.JdbcDatabase;
import dev.jobseeker.shared.text.FieldNames;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationSearchRepository {
    private final JdbcDatabase db;

    public ApplicationSearchRepository(JdbcDatabase db) {
        this.db = db;
    }

    public SearchRows find(ApplicationFilter filter) {
        String order =
                filter.sort().isEmpty()
                        ? (filter.q().isEmpty()
                                ? "a.updated_at DESC,a.id"
                                : "literal_match DESC,relevance DESC,a.updated_at DESC,a.id")
                        : "a."
                                + FieldNames.toColumnName(filter.sort())
                                + " "
                                + filter.direction()
                                + " NULLS LAST,a.id";
        var rows =
                db.rows(
                        "SELECT a.*,s.sections::text AS search_sections,"
                                + " coalesce(position(lower(:q) IN lower(s.search_document))>0,false)"
                                + " AS literal_match,"
                                + " ts_rank(s.search_vector,plainto_tsquery('simple',:q)) AS relevance"
                                + ApplicationSearchSql.FROM
                                + ApplicationSearchSql.WHERE
                                + " ORDER BY "
                                + order
                                + " LIMIT :limit OFFSET :offset",
                        ApplicationSearchSql.parameters(filter));

        long total =
                db.count(
                        "SELECT count(*)" + ApplicationSearchSql.FROM + ApplicationSearchSql.WHERE,
                        ApplicationSearchSql.parameters(filter));
        return new SearchRows(rows, total);
    }
}
