package dev.jobseeker.web.mvc.support;

import dev.jobseeker.shared.error.ApplicationException;
import dev.jobseeker.shared.validation.InputValidator;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FormRequestParser {
    private final FormValues forms;

    public FormRequestParser(FormValues forms) {
        this.forms = forms;
    }

    private Map<String, Object> values(Map<String, String> fields) {
        var body = new LinkedHashMap<String, Object>();
        fields.forEach(
                (key, value) -> {
                    if (!List.of("returnTo", "formId").contains(key))
                        body.put(key, value.isBlank() ? null : value);
                });
        for (String key : List.of("eventTime", "scheduledAt", "completedAt", "assessedAt"))
            if (body.get(key) != null) {
                try {
                    body.put(
                            key,
                            LocalDateTime.parse(body.get(key).toString())
                                    .atZone(forms.getZone())
                                    .toOffsetDateTime()
                                    .toString());
                } catch (RuntimeException e) {
                    throw ApplicationException.field(key, "Enter a valid local date and time.");
                }
            }
        return body;
    }

    public FormSubmission parse(Map<String, String> fields) {
        var values = values(fields);
        Object record = values.remove("recordId"), round = values.remove("parentRoundId");
        return new FormSubmission(
                values,
                record == null ? null : InputValidator.uuid(record, "reference"),
                round == null ? null : InputValidator.uuid(round, "reference"));
    }
}
