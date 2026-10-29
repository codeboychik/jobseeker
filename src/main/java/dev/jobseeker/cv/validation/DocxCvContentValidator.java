package dev.jobseeker.cv.validation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public final class DocxCvContentValidator {
    private static final String CONTENT_TYPES = "[Content_Types].xml";
    private static final String WORD_DOCUMENT = "word/document.xml";
    private static final Set<String> REQUIRED_PARTS = Set.of(CONTENT_TYPES, WORD_DOCUMENT);
    private static final int MAX_ENTRIES = 2000;
    private static final long MAX_EXPANDED_BYTES = 50L * 1024 * 1024;

    private DocxCvContentValidator() {}

    public static void validate(byte[] bytes) throws Exception {
        boolean contentTypes = false, document = false;
        int entries = 0;
        long expanded = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new IOException("Too many DOCX entries");
                var data = readEntry(zip, REQUIRED_PARTS.contains(entry.getName()), expanded);
                expanded = data.expandedBytes();
                if (!REQUIRED_PARTS.contains(entry.getName())) continue;
                Document xml = parseXml(data.content());
                if (CONTENT_TYPES.equals(entry.getName())) contentTypes = hasContentTypes(xml);
                else document = isWordDocument(xml);
            }
        }
        if (!contentTypes || !document) throw new IOException("Required DOCX parts are missing");
    }

    private static ExpandedEntry readEntry(ZipInputStream zip, boolean retain, long expanded)
            throws IOException {
        var content = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = zip.read(buffer)) != -1) {
            expanded += count;
            if (expanded > MAX_EXPANDED_BYTES)
                throw new IOException("DOCX expansion limit exceeded");
            if (retain) content.write(buffer, 0, count);
        }
        return new ExpandedEntry(content.toByteArray(), expanded);
    }

    private static Document parseXml(byte[] content) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
    }

    private static boolean hasContentTypes(Document xml) {
        return xml.getElementsByTagNameNS(
                                "http://schemas.openxmlformats.org/package/2006/content-types",
                                "Override")
                        .getLength()
                > 0;
    }

    private static boolean isWordDocument(Document xml) {
        var root = xml.getDocumentElement();
        return "document".equals(root.getLocalName())
                && "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        .equals(root.getNamespaceURI());
    }

    private record ExpandedEntry(byte[] content, long expandedBytes) {}
}
