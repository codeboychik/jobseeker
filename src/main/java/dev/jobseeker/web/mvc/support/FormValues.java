package dev.jobseeker.web.mvc.support;

import dev.jobseeker.shared.text.DisplayLabels;
import dev.jobseeker.shared.text.FieldNames;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component("forms")
public class FormValues {
    private final ZoneId zone;

    public FormValues(@Value("${jobseeker.timezone}") String zone) {
        this.zone = ZoneId.of(zone);
    }

    public Object value(String form, String field, Object original) {
        var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attrs.getRequest();
        if (form.equals(request.getAttribute("failedForm"))
                && request.getAttribute("submitted") instanceof Map<?, ?> submitted
                && submitted.containsKey(field)) return submitted.get(field);
        if (original instanceof Instant time)
            return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").format(time.atZone(zone));
        return original;
    }

    public String label(Object value) {
        return DisplayLabels.label(value);
    }

    public String fieldLabel(String value) {
        return DisplayLabels.label(FieldNames.toColumnName(value));
    }

    public boolean failed(String form) {
        var request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                        .getRequest();
        return form.equals(request.getAttribute("failedForm"));
    }

    public String time(Object value) {
        if (value == null) return "Unknown";
        if (value instanceof Instant instant)
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").format(instant.atZone(zone));
        return value.toString();
    }

    public ZoneId getZone() {
        return zone;
    }
}
