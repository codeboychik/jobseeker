package dev.jobseeker.web.api;

import dev.jobseeker.application.ApplicationDetailService;
import dev.jobseeker.application.ApplicationService;
import dev.jobseeker.idempotency.IdempotentResponse;
import dev.jobseeker.search.ApplicationFilter;
import dev.jobseeker.search.ApplicationSearchService;
import dev.jobseeker.shared.audit.InputChannel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApplicationApiController {
    private final ApplicationService applications;
    private final ApplicationDetailService details;
    private final ApplicationSearchService search;
    private final ApiResponseFactory responses;

    public ApplicationApiController(
            ApplicationService applications,
            ApplicationDetailService details,
            ApplicationSearchService search,
            ApiResponseFactory responses) {
        this.applications = applications;
        this.details = details;
        this.search = search;
        this.responses = responses;
    }

    @GetMapping("/applications")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "active") String archive,
            @RequestParam(defaultValue = "") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return search.list(new ApplicationFilter(q, status, archive, sort, direction, page, size));
    }

    @GetMapping("/applications/{id}")
    public Map<String, Object> detail(@PathVariable UUID id) {
        return details.detail(id);
    }

    @PostMapping("/applications")
    public ResponseEntity<?> create(
            @RequestBody Map<String, Object> input, HttpServletRequest request) {
        return responses.post(
                request,
                input,
                () -> {
                    UUID id = applications.create(input, InputChannel.AUTOMATION);
                    return new IdempotentResponse(
                            201, details.detail(id), "/api/v1/applications/" + id);
                });
    }

    @PatchMapping("/applications/{id}")
    public Map<String, Object> update(
            @PathVariable UUID id, @RequestBody Map<String, Object> input) {
        applications.update(id, input, InputChannel.AUTOMATION);
        return details.detail(id);
    }

    @GetMapping("/applications/{id}/status-changes")
    public Object history(@PathVariable UUID id) {
        return applications.history(id);
    }

    @PostMapping("/applications/{id}/status-changes")
    public ResponseEntity<?> status(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> input,
            HttpServletRequest request) {
        return responses.post(
                request,
                input,
                () -> {
                    applications.status(id, input, InputChannel.AUTOMATION);
                    return new IdempotentResponse(200, details.detail(id), null);
                });
    }

    @PutMapping("/applications/{id}/archive")
    public Object archive(@PathVariable UUID id, @RequestBody Map<String, Object> input) {
        applications.archive(id, input, InputChannel.AUTOMATION);
        return details.detail(id);
    }
}
