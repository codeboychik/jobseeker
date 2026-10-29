package dev.jobseeker.application;

import dev.jobseeker.cv.CvAttachmentService;
import dev.jobseeker.interview.InterviewRoundRepository;
import dev.jobseeker.interview.QuestionAnswerRepository;
import dev.jobseeker.note.NoteRepository;
import dev.jobseeker.rejection.RejectionReasonRepository;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ApplicationDetailService {
    private final ApplicationRepository applications;
    private final StatusHistoryRepository history;
    private final InterviewRoundRepository rounds;
    private final QuestionAnswerRepository questions;
    private final NoteRepository notes;
    private final CvAttachmentService attachments;
    private final RejectionReasonRepository reasons;

    public ApplicationDetailService(
            ApplicationRepository applications,
            StatusHistoryRepository history,
            InterviewRoundRepository rounds,
            QuestionAnswerRepository questions,
            NoteRepository notes,
            CvAttachmentService attachments,
            RejectionReasonRepository reasons) {
        this.applications = applications;
        this.history = history;
        this.rounds = rounds;
        this.questions = questions;
        this.notes = notes;
        this.attachments = attachments;
        this.reasons = reasons;
    }

    public Map<String, Object> detail(UUID id) {
        var result = applications.find(id);
        result.put("statusChanges", history.findByApplication(id));
        result.put("cvAttachments", attachments.list(id));
        var applicationRounds = rounds.findByApplication(id);
        var applicationNotes = notes.findByApplication(id);
        var applicationQuestions = questions.findByApplication(id);
        for (var round : applicationRounds) {
            round.put(
                    "questions",
                    applicationQuestions.stream()
                            .filter(q -> q.get("roundId").equals(round.get("id")))
                            .toList());
            round.put(
                    "notes",
                    applicationNotes.stream()
                            .filter(n -> Objects.equals(n.get("roundId"), round.get("id")))
                            .toList());
        }
        result.put("rounds", applicationRounds);
        result.put("notes", applicationNotes);
        result.put("rejectionReasons", reasons.findByApplication(id));
        result.put("duplicateCount", applications.duplicateCount(result));
        return result;
    }
}
