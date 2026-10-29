package dev.jobseeker.web.api;

import dev.jobseeker.application.ApplicationDetailService;
import dev.jobseeker.interview.QuestionAnswerService;
import dev.jobseeker.shared.audit.InputChannel;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QuestionAnswerApiController {
    private final ApplicationDetailService details;
    private final QuestionAnswerService questions;
    private final ApiResponseFactory responses;

    public QuestionAnswerApiController(
            ApplicationDetailService details,
            QuestionAnswerService questions,
            ApiResponseFactory responses) {
        this.details = details;
        this.questions = questions;
        this.responses = responses;
    }

    @GetMapping("/applications/{id}/rounds/{round}/questions")
    public Object questions(@PathVariable UUID id, @PathVariable UUID round) {
        return questions.list(id, round);
    }

    @PostMapping("/applications/{id}/rounds/{round}/questions")
    public ResponseEntity<?> addQuestion(
            @PathVariable UUID id,
            @PathVariable UUID round,
            @RequestBody Map<String, Object> input,
            HttpServletRequest request) {
        return responses.post(
                request,
                input,
                () ->
                        responses.created(
                                questions.create(id, round, input, InputChannel.AUTOMATION),
                                id,
                                request.getRequestURI()));
    }

    @PatchMapping("/applications/{id}/rounds/{round}/questions/{question}")
    public Object editQuestion(
            @PathVariable UUID id,
            @PathVariable UUID round,
            @PathVariable UUID question,
            @RequestBody Map<String, Object> input) {
        questions.update(id, round, question, input, InputChannel.AUTOMATION);
        return details.detail(id);
    }

    @DeleteMapping("/applications/{id}/rounds/{round}/questions/{question}")
    public Object deleteQuestion(
            @PathVariable UUID id,
            @PathVariable UUID round,
            @PathVariable UUID question,
            @RequestBody Map<String, Object> input) {
        questions.delete(id, round, question, input, InputChannel.AUTOMATION);
        return details.detail(id);
    }
}
