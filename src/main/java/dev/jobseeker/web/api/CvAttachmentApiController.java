package dev.jobseeker.web.api;

import dev.jobseeker.application.ApplicationDetailService;
import dev.jobseeker.cv.CvAttachmentService;
import dev.jobseeker.cv.CvFileValidator;
import dev.jobseeker.shared.audit.InputChannel;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class CvAttachmentApiController {
    private final ApplicationDetailService details;
    private final CvAttachmentService attachments;
    private final CvFileValidator files;
    private final ApiResponseFactory responses;

    public CvAttachmentApiController(
            ApplicationDetailService details,
            CvAttachmentService attachments,
            CvFileValidator files,
            ApiResponseFactory responses) {
        this.details = details;
        this.attachments = attachments;
        this.files = files;
        this.responses = responses;
    }

    @GetMapping("/applications/{id}/cv-attachments")
    public Object cvs(@PathVariable UUID id) {
        return attachments.list(id);
    }

    @PostMapping(
            value = "/applications/{id}/cv-attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable UUID id,
            @RequestParam MultipartFile file,
            @RequestParam Map<String, String> fields,
            HttpServletRequest request)
            throws IOException {
        var validated = files.validate(file.getOriginalFilename(), file.getBytes());
        var input = new LinkedHashMap<String, Object>(fields);
        input.remove("file");
        var hash = new LinkedHashMap<>(input);
        hash.put("fileChecksum", validated.checksum());
        hash.put("filename", validated.filename());
        return responses.post(
                request,
                hash,
                () ->
                        responses.created(
                                attachments.upload(id, validated, input, InputChannel.AUTOMATION),
                                id,
                                request.getRequestURI()));
    }

    @PatchMapping("/applications/{id}/cv-attachments/{cv}")
    public Object editCv(
            @PathVariable UUID id, @PathVariable UUID cv, @RequestBody Map<String, Object> input) {
        attachments.update(id, cv, input, InputChannel.AUTOMATION);
        return details.detail(id);
    }

    @GetMapping("/applications/{id}/cv-attachments/{cv}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, @PathVariable UUID cv) {
        var file = attachments.download(id, cv);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.get("mediaType").toString()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.get("filename").toString(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .body((byte[]) file.get("content"));
    }
}
