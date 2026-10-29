package dev.jobseeker.web.api;

import dev.jobseeker.rejection.RejectionReasonService;
import dev.jobseeker.shared.audit.InputChannel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RejectionReasonApiController {
    private final RejectionReasonService reasons;
    private final ApiResponseFactory responses;

    public RejectionReasonApiController(
            RejectionReasonService reasons, ApiResponseFactory responses) {
        this.reasons = reasons;
        this.responses = responses;
    }

    @GetMapping("/applications/{id}/rejection-reasons")
    public Object reasons(@PathVariable UUID id) {
        return reasons.list(id);
    }

    @PostMapping("/applications/{id}/rejection-reasons")
    public ResponseEntity<?> addReason(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> input,
            HttpServletRequest request) {
        return responses.post(
                request,
                input,
                () ->
                        responses.created(
                                reasons.create(id, input, InputChannel.AUTOMATION),
                                id,
                                request.getRequestURI()));
    }
}
