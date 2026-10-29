package dev.jobseeker.cv.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;

public final class PdfCvContentValidator {
    private PdfCvContentValidator() {}

    public static void validate(byte[] bytes) throws IOException {
        String signature =
                new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII);
        if (!signature.equals("%PDF-")) throw new IOException("Invalid PDF signature");
        try (var document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() == 0) throw new IOException("PDF has no pages");
        }
    }
}
