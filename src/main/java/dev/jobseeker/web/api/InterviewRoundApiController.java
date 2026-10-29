package dev.jobseeker.web.api;

import dev.jobseeker.application.ApplicationDetailService;
import dev.jobseeker.interview.InterviewRoundService;
import dev.jobseeker.shared.audit.InputChannel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InterviewRoundApiController {
    private final ApplicationDetailService details;
    private final InterviewRoundService rounds;
    private final ApiResponseFactory responses;

    public InterviewRoundApiController(
            ApplicationDetailService details,
            InterviewRoundService rounds,
            ApiResponseFactory responses) {
        this.details = details;
        this.rounds = rounds;
        this.responses = responses;
    }

    @GetMapping("/applications/{id}/rounds")
    public Object rounds(@PathVariable UUID id) {
        return rounds.list(id);
    }

    @PostMapping("/applications/{id}/rounds")
    public ResponseEntity<?> addRound(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> input,
            HttpServletRequest request) {
        return responses.post(
                request,
                input,
                () ->
                        responses.created(
                                rounds.create(id, input, InputChannel.AUTOMATION),
                                id,
                                request.getRequestURI()));
    }

    @PatchMapping("/applications/{id}/rounds/{round}")
    public Object editRound(
            @PathVariable UUID id,
            @PathVariable UUID round,
            @RequestBody Map<String, Object> input) {
        rounds.update(id, round, input, InputChannel.AUTOMATION);
        return details.detail(id);
    }
}
