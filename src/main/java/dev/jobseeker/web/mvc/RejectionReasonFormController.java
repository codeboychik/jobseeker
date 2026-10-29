package dev.jobseeker.web.mvc;

import dev.jobseeker.rejection.RejectionReasonService;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.web.mvc.support.FormResult;
import dev.jobseeker.web.mvc.support.FormSubmissionHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class RejectionReasonFormController {
    private final FormSubmissionHandler submissions;
    private final RejectionReasonService reasons;

    public RejectionReasonFormController(
            FormSubmissionHandler submissions, RejectionReasonService reasons) {
        this.submissions = submissions;
        this.reasons = reasons;
    }

    @PostMapping("/applications/{id}/reason")
    public ModelAndView reason(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    reasons.create(id, submission.values(), InputChannel.MANUAL);
                    return new FormResult(id, "reasons");
                });
    }
}
