package dev.jobseeker.web.api;

import dev.jobseeker.application.ApplicationDetailService;
import dev.jobseeker.idempotency.IdempotencyService;
import dev.jobseeker.idempotency.IdempotentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {
    private final ApplicationDetailService details;
    private final IdempotencyService idempotency;

    public ApiResponseFactory(ApplicationDetailService details, IdempotencyService idempotency) {
        this.details = details;
        this.idempotency = idempotency;
    }

    public IdempotentResponse created(UUID resource, UUID app, String collection) {
        var detail = details.detail(app);
        return new IdempotentResponse(
                201,
                Map.of(
                        "id", resource,
                        "applicationVersion", detail.get("version"),
                        "application", detail),
                collection + "/" + resource);
    }

    public ResponseEntity<?> post(
            HttpServletRequest request, Object payload, Supplier<IdempotentResponse> action) {
        var result =
                idempotency.execute(
                        request.getHeader("Idempotency-Key"),
                        request.getRequestURI(),
                        payload,
                        action);
        var builder = ResponseEntity.status(result.status());
        if (result.location() != null) builder.header(HttpHeaders.LOCATION, result.location());
        return builder.body(result.body());
    }
}
