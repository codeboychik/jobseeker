package dev.jobseeker.web.error;

import dev.jobseeker.shared.error.ApplicationException;
import java.util.LinkedHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<?> problem(ApplicationException problem) {
        var body = new LinkedHashMap<String, Object>();
        body.put("status", problem.status());
        body.put("message", problem.getMessage());
        body.put("fields", problem.fields());
        if (problem.current() != null) body.put("current", problem.current());
        return ResponseEntity.status(problem.status()).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity() {
        return problem(
                new ApplicationException(
                        409,
                        "This change conflicts with an existing record or reference. Reload and"
                                + " check the values."));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<?> badInput() {
        return problem(
                new ApplicationException(400, "Invalid JSON, identifier, or query parameter."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> uploadSize() {
        return problem(
                new ApplicationException(413, "The upload exceeds the configured size limit."));
    }
}
