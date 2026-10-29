package dev.jobseeker.web.api;

import dev.jobseeker.catalog.LookupKind;
import dev.jobseeker.catalog.LookupTypeService;
import dev.jobseeker.idempotency.IdempotentResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LookupTypeApiController {
    private final LookupTypeService lookups;
    private final ApiResponseFactory responses;

    public LookupTypeApiController(LookupTypeService lookups, ApiResponseFactory responses) {
        this.lookups = lookups;
        this.responses = responses;
    }

    @GetMapping("/round-types")
    public Object roundTypes() {
        return lookups.list(LookupKind.ROUND);
    }

    @GetMapping("/rejection-reason-types")
    public Object reasonTypes() {
        return lookups.list(LookupKind.REJECTION_REASON);
    }

    @PostMapping({"/round-types", "/rejection-reason-types"})
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> input, HttpServletRequest request) {
        LookupKind kind =
                request.getRequestURI().endsWith("/round-types")
                        ? LookupKind.ROUND
                        : LookupKind.REJECTION_REASON;
        return responses.post(
                request,
                input,
                () -> {
                    UUID id = lookups.create(kind, input);
                    return new IdempotentResponse(
                            201,
                            lookups.list(kind).stream()
                                    .filter(row -> id.equals(row.get("id")))
                                    .findFirst()
                                    .orElseThrow(),
                            request.getRequestURI());
                });
    }
}
