package dev.jobseeker.web.mvc;

import dev.jobseeker.note.NoteService;
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
public class NoteFormController {
    private final FormSubmissionHandler submissions;
    private final NoteService notes;

    public NoteFormController(FormSubmissionHandler submissions, NoteService notes) {
        this.submissions = submissions;
        this.notes = notes;
    }

    @PostMapping("/applications/{id}/note")
    public ModelAndView note(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    if (submission.recordId() == null)
                        notes.create(id, submission.values(), InputChannel.MANUAL);
                    else
                        notes.update(
                                id,
                                submission.recordId(),
                                submission.values(),
                                InputChannel.MANUAL);
                    return new FormResult(
                            id,
                            submission.roundId() == null
                                    ? "notes"
                                    : "round-" + submission.roundId());
                });
    }
}
