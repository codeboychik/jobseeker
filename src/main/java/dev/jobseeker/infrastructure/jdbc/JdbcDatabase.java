package dev.jobseeker.infrastructure.jdbc;

import dev.jobseeker.shared.error.ApplicationException;
import dev.jobseeker.shared.text.FieldNames;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDatabase {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcDatabase(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> rows(String sql, Map<String, ?> params) {
        return jdbc.query(
                sql,
                params,
                (rs, n) -> {
                    var row = new LinkedHashMap<String, Object>();
                    var meta = rs.getMetaData();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof Timestamp timestamp) value = timestamp.toInstant();
                        else if (value instanceof java.sql.Date date) value = date.toLocalDate();
                        row.put(FieldNames.toPropertyName(meta.getColumnLabel(i)), value);
                    }
                    return row;
                });
    }

    public Map<String, Object> one(String sql, Map<String, ?> params) {
        return rows(sql, params).stream()
                .findFirst()
                .orElseThrow(() -> new ApplicationException(404, "Record not found."));
    }

    public int execute(String sql, Map<String, ?> params) {
        return jdbc.update(sql, params);
    }

    public long count(String sql, Map<String, ?> params) {
        return ((Number) one(sql, params).values().iterator().next()).longValue();
    }

    // Table/column names come exclusively from internal schemas, never request values.
    public void insert(String table, Map<String, Object> values) {
        execute(
                "INSERT INTO "
                        + table
                        + " ("
                        + String.join(",", values.keySet())
                        + ") VALUES ("
                        + String.join(",", values.keySet().stream().map(k -> ":" + k).toList())
                        + ")",
                values);
    }

    public void update(String table, UUID id, Map<String, Object> values) {
        if (values.isEmpty()) return;
        var params = new HashMap<>(values);
        params.put("recordId", id);
        execute(
                "UPDATE "
                        + table
                        + " SET "
                        + String.join(",", values.keySet().stream().map(k -> k + "=:" + k).toList())
                        + " WHERE id=:recordId",
                params);
    }

    public void updateVersioned(String table, UUID id, Map<String, Object> values) {
        update(table, id, values);
        execute(
                "UPDATE "
                        + table
                        + " SET version=version+1,updated_at=clock_timestamp() WHERE id=:id",
                Map.of("id", id));
    }
}
