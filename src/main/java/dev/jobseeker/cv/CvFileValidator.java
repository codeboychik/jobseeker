package dev.jobseeker.cv;

import dev.jobseeker.cv.validation.DocxCvContentValidator;
import dev.jobseeker.cv.validation.PdfCvContentValidator;
import dev.jobseeker.shared.crypto.Checksums;
import dev.jobseeker.shared.error.ApplicationException;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CvFileValidator {
    private final long limit;

    public CvFileValidator(@Value("${jobseeker.upload-limit}") long limit) {
        this.limit = limit;
    }

    public ValidatedCv validate(String filename, byte[] bytes) {
        validateSize(bytes);
        String name = normalizeFilename(filename);
        String mediaType;
        try {
            mediaType = validateContent(name, bytes);
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApplicationException.field(
                    "file",
                    "This is not a readable PDF or DOCX file (password-protected PDFs are not"
                            + " supported).");
        }
        return new ValidatedCv(name, mediaType, bytes.length, Checksums.sha256(bytes), bytes);
    }

    private void validateSize(byte[] bytes) {
        if (bytes.length == 0)
            throw ApplicationException.field("file", "Choose a non-empty PDF or DOCX file.");
        if (bytes.length > limit)
            throw new ApplicationException(413, "The file exceeds the configured upload limit.");
    }

    private static String normalizeFilename(String filename) {
        String name = Objects.toString(filename, "cv").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
        return name.length() > 240 ? name.substring(name.length() - 240) : name;
    }

    private static String validateContent(String filename, byte[] bytes) throws Exception {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            PdfCvContentValidator.validate(bytes);
            return "application/pdf";
        }
        if (lower.endsWith(".docx")) {
            DocxCvContentValidator.validate(bytes);
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        throw new ApplicationException(415, "Only PDF and DOCX files are supported.");
    }
}
