package dev.jobseeker.web.mvc;

import dev.jobseeker.catalog.LookupKind;
import dev.jobseeker.catalog.LookupTypeService;
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
public class LookupTypeFormController {
    private final FormSubmissionHandler submissions;
    private final LookupTypeService lookups;

    public LookupTypeFormController(FormSubmissionHandler submissions, LookupTypeService lookups) {
        this.submissions = submissions;
        this.lookups = lookups;
    }

    @PostMapping("/applications/{id}/round-type")
    public ModelAndView roundType(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    lookups.create(LookupKind.ROUND, submission.values());
                    return new FormResult(id, "rounds");
                });
    }

    @PostMapping("/applications/{id}/reason-type")
    public ModelAndView reasonType(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    lookups.create(LookupKind.REJECTION_REASON, submission.values());
                    return new FormResult(id, "reasons");
                });
    }
}
