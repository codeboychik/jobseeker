package dev.jobseeker.web.mvc;

import dev.jobseeker.application.ApplicationService;
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
public class ApplicationFormController {
    private final FormSubmissionHandler submissions;
    private final ApplicationService applications;

    public ApplicationFormController(
            FormSubmissionHandler submissions, ApplicationService applications) {
        this.submissions = submissions;
        this.applications = applications;
    }

    @PostMapping("/applications/{id}/edit")
    public ModelAndView edit(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    applications.update(id, submission.values(), InputChannel.MANUAL);
                    return new FormResult(id, "overview");
                });
    }

    @PostMapping("/applications/{id}/status")
    public ModelAndView status(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    applications.status(id, submission.values(), InputChannel.MANUAL);
                    return new FormResult(id, "history");
                });
    }

    @PostMapping("/applications/{id}/archive")
    public ModelAndView archive(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    applications.archive(id, submission.values(), InputChannel.MANUAL);
                    return new FormResult(id, "overview");
                });
    }

    @PostMapping("/applications")
    public ModelAndView create(@RequestParam Map<String, String> fields, HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                null,
                fields,
                request,
                submission ->
                        new FormResult(
                                applications.create(submission.values(), InputChannel.MANUAL),
                                "overview"));
    }
}
