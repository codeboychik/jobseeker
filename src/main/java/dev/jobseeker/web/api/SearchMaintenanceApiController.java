package dev.jobseeker.web.api;

import dev.jobseeker.idempotency.IdempotentResponse;
import dev.jobseeker.search.SearchIndexService;
import dev.jobseeker.shared.validation.InputValidator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SearchMaintenanceApiController {
    private final SearchIndexService index;
    private final ApiResponseFactory responses;

    public SearchMaintenanceApiController(SearchIndexService index, ApiResponseFactory responses) {
        this.index = index;
        this.responses = responses;
    }

    @PostMapping("/search/rebuild")
    public ResponseEntity<?> rebuild(
            @RequestBody Map<String, Object> input, HttpServletRequest request) {
        InputValidator.keys(input, Map.of());
        return responses.post(
                request,
                input,
                () ->
                        new IdempotentResponse(
                                200, Map.of("applicationsIndexed", index.rebuild()), null));
    }
}
