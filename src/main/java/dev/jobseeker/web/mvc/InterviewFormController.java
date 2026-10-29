package dev.jobseeker.web.mvc;

import dev.jobseeker.interview.InterviewRoundService;
import dev.jobseeker.interview.QuestionAnswerService;
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
import org.springframework.web.servlet.ModelAndView;

@Controller
public class InterviewFormController {
    private final FormSubmissionHandler submissions;
    private final InterviewRoundService rounds;
    private final QuestionAnswerService questions;

    public InterviewFormController(
            FormSubmissionHandler submissions,
            InterviewRoundService rounds,
            QuestionAnswerService questions) {
        this.submissions = submissions;
        this.rounds = rounds;
        this.questions = questions;
    }

    @PostMapping("/applications/{id}/round")
    public ModelAndView round(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    UUID round = submission.recordId();
                    if (round == null)
                        round = rounds.create(id, submission.values(), InputChannel.MANUAL);
                    else rounds.update(id, round, submission.values(), InputChannel.MANUAL);
                    return new FormResult(id, "round-" + round);
                });
    }

    @PostMapping("/applications/{id}/question")
    public ModelAndView question(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    if (submission.roundId() == null)
                        throw ApplicationException.field("round", "Choose a round.");
                    if (submission.recordId() == null)
                        questions.create(
                                id, submission.roundId(), submission.values(), InputChannel.MANUAL);
                    else
                        questions.update(
                                id,
                                submission.roundId(),
                                submission.recordId(),
                                submission.values(),
                                InputChannel.MANUAL);
                    return new FormResult(id, "round-" + submission.roundId());
                });
    }

    @PostMapping("/applications/{id}/delete-question")
    public ModelAndView deleteQuestion(
            @PathVariable UUID id,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        return submissions.submit(
                id,
                fields,
                request,
                submission -> {
                    questions.delete(
                            id,
                            submission.roundId(),
                            submission.recordId(),
                            submission.values(),
                            InputChannel.MANUAL);
                    return new FormResult(id, "round-" + submission.roundId());
                });
    }
}
