package dev.jobseeker.search;

import dev.jobseeker.infrastructure.json.JsonCodec;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchExcerptBuilder {
    private final JsonCodec json;

    public SearchExcerptBuilder(JsonCodec json) {
        this.json = json;
    }

    public void apply(Map<String, Object> row, String q) {
        Object raw = row.remove("searchSections");
        row.remove("literalMatch");
        row.remove("relevance");
        row.put("matchAnchor", "overview");
        row.put("matchSection", "");
        row.put("matchExcerpt", "");
        if (q.isEmpty() || raw == null) return;
        for (var section : json.list(raw.toString())) {
            String text = section.get("text").toString();
            String lower = text.toLowerCase(Locale.ROOT);
            int at = lower.indexOf(q.toLowerCase(Locale.ROOT));
            if (at < 0)
                for (String word : q.toLowerCase(Locale.ROOT).split("\\s+")) {
                    at = lower.indexOf(word);
                    if (at >= 0) break;
                }
            if (at >= 0) {
                int start = Math.max(0, at - 45), end = Math.min(text.length(), at + 180);
                row.put("matchAnchor", section.get("anchor"));
                row.put("matchSection", section.get("title"));
                row.put(
                        "matchExcerpt",
                        (start > 0 ? "…" : "")
                                + text.substring(start, end).replace('\n', ' ')
                                + (end < text.length() ? "…" : ""));
                return;
            }
        }
    }
}
