package dev.jobseeker.search;

import dev.jobseeker.shared.text.DisplayLabels;
import dev.jobseeker.shared.text.FieldNames;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SearchDocumentBuilder {
    private final SearchIndexRepository repository;

    public SearchDocumentBuilder(SearchIndexRepository repository) {
        this.repository = repository;
    }

    public SearchDocument build(UUID id) {
        var sections = new ArrayList<Map<String, Object>>();
        var app = repository.application(id);
        add(sections, "Application", "overview", app);
        repository.history(id).forEach(row -> add(sections, "Status history", "history", row));
        repository.attachments(id).forEach(row -> add(sections, "CV", "cv", row));
        repository
                .rounds(id)
                .forEach(
                        row ->
                                add(
                                        sections,
                                        "Round: " + row.get("typeName"),
                                        "round-" + row.get("id"),
                                        row));
        repository
                .questions(id)
                .forEach(
                        row ->
                                add(
                                        sections,
                                        "Question / answer: " + row.get("typeName"),
                                        "round-" + row.get("roundId"),
                                        row));
        repository
                .notes(id)
                .forEach(
                        row ->
                                add(
                                        sections,
                                        "Note",
                                        row.get("roundId") == null
                                                ? "notes"
                                                : "round-" + row.get("roundId"),
                                        row));
        repository.reasons(id).forEach(row -> add(sections, "Rejection reason", "reasons", row));
        // Normalize in Java too: a database initialized with the C locale only folds ASCII.
        String document =
                String.join(
                                "\n\n",
                                sections.stream()
                                        .map(s -> s.get("title") + ": " + s.get("text"))
                                        .toList())
                        .toLowerCase(Locale.ROOT);
        return new SearchDocument(
                id, document, sections, ((Number) app.get("version")).longValue());
    }

    private void add(
            List<Map<String, Object>> sections,
            String title,
            String anchor,
            Map<String, Object> row) {
        String text =
                String.join(
                        "\n",
                        row.entrySet().stream()
                                .filter(entry -> entry.getValue() != null)
                                .map(this::formatField)
                                .toList());
        sections.add(Map.of("title", title, "anchor", anchor, "text", text));
    }

    private String formatField(Map.Entry<String, Object> entry) {
        String value = entry.getValue().toString();
        String label = DisplayLabels.label(FieldNames.toColumnName(entry.getKey()));
        String alternative =
                value.contains("_") && value.matches("[A-Z_]+")
                        ? " (" + DisplayLabels.label(value) + ")"
                        : "";
        return label + ": " + value + alternative;
    }
}
