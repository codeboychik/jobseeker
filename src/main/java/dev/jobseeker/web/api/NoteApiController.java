package dev.jobseeker.web.api;

import dev.jobseeker.application.ApplicationDetailService;
import dev.jobseeker.note.NoteService;
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
public class NoteApiController {
    private final ApplicationDetailService details;
    private final NoteService notes;
    private final ApiResponseFactory responses;

    public NoteApiController(
            ApplicationDetailService details, NoteService notes, ApiResponseFactory responses) {
        this.details = details;
        this.notes = notes;
        this.responses = responses;
    }

    @GetMapping("/applications/{id}/notes")
    public Object notes(@PathVariable UUID id) {
        return notes.list(id);
    }

    @PostMapping("/applications/{id}/notes")
    public ResponseEntity<?> addNote(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> input,
            HttpServletRequest request) {
        return responses.post(
                request,
                input,
                () ->
                        responses.created(
                                notes.create(id, input, InputChannel.AUTOMATION),
                                id,
                                request.getRequestURI()));
    }

    @PatchMapping("/applications/{id}/notes/{note}")
    public Object editNote(
            @PathVariable UUID id,
            @PathVariable UUID note,
            @RequestBody Map<String, Object> input) {
        notes.update(id, note, input, InputChannel.AUTOMATION);
        return details.detail(id);
    }
}
