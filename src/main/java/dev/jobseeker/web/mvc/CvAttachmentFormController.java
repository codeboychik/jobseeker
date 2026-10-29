package dev.jobseeker.web.mvc;

import dev.jobseeker.cv.CvAttachmentService;
import dev.jobseeker.cv.CvFileValidator;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.shared.error.ApplicationException;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CvAttachmentFormController {
    private final FormSubmissionHandler submissions;
    private final CvAttachmentService attachments;
    private final CvFileValidator files;

    public CvAttachmentFormController(
            FormSubmissionHandler submissions,
            CvAttachmentService attachments,
            CvFileValidator files) {
        this.submissions = submissions;
        this.attachments = attachments;
        this.files = files;
    }

    @PostMapping("/applications/{id}/cv")
    public ModelAndView cv(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    attachments.update(
                            id, submission.recordId(), submission.values(), InputChannel.MANUAL);
                    return new FormResult(id, "cv");
                });
    }

    @PostMapping("/applications/{id}/upload")
    public ModelAndView upload(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            @RequestParam(required = false) MultipartFile file,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    if (file == null) throw ApplicationException.field("file", "Choose a file.");
                    submission.values().remove("file");
                    attachments.upload(
                            id,
                            files.validate(file.getOriginalFilename(), file.getBytes()),
                            submission.values(),
                            InputChannel.MANUAL);
                    return new FormResult(id, "cv");
                });
    }
}
