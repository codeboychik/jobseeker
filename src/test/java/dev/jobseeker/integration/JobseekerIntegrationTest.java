package dev.jobseeker.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.jobseeker.cv.CvAttachmentService;
import dev.jobseeker.cv.CvFileValidator;
import dev.jobseeker.idempotency.IdempotencyService;
import dev.jobseeker.infrastructure.json.JsonCodec;
import dev.jobseeker.search.SearchIndexService;
import dev.jobseeker.shared.audit.InputChannel;
import dev.jobseeker.shared.crypto.Checksums;
import dev.jobseeker.shared.error.ApplicationException;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobseekerIntegrationTest {
    private static final String ADMIN =
            System.getenv()
                    .getOrDefault("TEST_PG_URL", "jdbc:postgresql://127.0.0.1:55432/postgres");
    private static final String USER = System.getenv().getOrDefault("TEST_PG_USER", "jobseeker"),
            PASSWORD = System.getenv().getOrDefault("TEST_PG_PASSWORD", "jobseeker");
    private static final String DATABASE =
            "jobseeker_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = ADMIN.substring(0, ADMIN.lastIndexOf('/') + 1) + DATABASE;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        try (var c = DriverManager.getConnection(ADMIN, USER, PASSWORD);
                var s = c.createStatement()) {
            s.execute("CREATE DATABASE " + DATABASE);
        }
        registry.add("spring.datasource.url", () -> URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    @Autowired Environment environment;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonCodec json;
    @Autowired CvAttachmentService attachments;
    @Autowired IdempotencyService idempotency;
    @Autowired CvFileValidator validator;
    @Autowired SearchIndexService search;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    record Reply(int status, String text, HttpHeaders headers) {}

    @BeforeEach
    void clean() {
        assertTrue(URL.endsWith(DATABASE) && DATABASE.startsWith("jobseeker_test_"));
        jdbc.execute(
                "TRUNCATE"
                        + " application_search,rejection_reasons,questions,notes,interview_rounds,cv_attachments,stored_files,status_changes,applications,idempotency_records"
                        + " CASCADE");
        jdbc.execute("DELETE FROM round_types WHERE NOT predefined");
        jdbc.execute("DELETE FROM rejection_reason_types WHERE NOT predefined");
    }

    @AfterAll
    static void drop() throws Exception {
        try (var c = DriverManager.getConnection(ADMIN, USER, PASSWORD);
                var s = c.createStatement()) {
            s.execute("DROP DATABASE " + DATABASE + " WITH (FORCE)");
        }
    }

    private String base() {
        return "http://127.0.0.1:" + environment.getProperty("local.server.port");
    }

    private Reply send(String method, String path, Object body, String key) throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create(base() + path)).timeout(Duration.ofSeconds(30));
        if (key != null) builder.header("Idempotency-Key", key);
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.write(body)));
        var r = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(r.statusCode(), r.body(), r.headers());
    }

    private Map<String, Object> post(String path, Map<String, Object> body) throws Exception {
        var reply = send("POST", "/api/v1" + path, body, UUID.randomUUID().toString());
        assertTrue(reply.status == 200 || reply.status == 201, reply.text);
        return json.object(reply.text);
    }

    private Map<String, Object> create(String company) throws Exception {
        return post("/applications", Map.of("company", company, "role", "Backend engineer"));
    }

    private Map<String, Object> get(String id) throws Exception {
        var r = send("GET", "/api/v1/applications/" + id, null, null);
        assertEquals(200, r.status, r.text);
        return json.object(r.text);
    }

    private long version(String id) throws Exception {
        return ((Number) get(id).get("version")).longValue();
    }

    private Map<String, Object> child(String id, String path, Map<String, Object> values)
            throws Exception {
        var body = new HashMap<>(values);
        body.put("applicationVersion", version(id));
        return post("/applications/" + id + path, body);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private long matches(String query) throws Exception {
        var r =
                send(
                        "GET",
                        "/api/v1/applications?q="
                                + URLEncoder.encode(query, StandardCharsets.UTF_8),
                        null,
                        null);
        assertEquals(200, r.status, r.text);
        return ((Number) json.object(r.text).get("total")).longValue();
    }

    private Map<String, Object> summary() throws Exception {
        return json.object(send("GET", "/api/v1/statistics/summary", null, null).text);
    }

    private void status(String id, String status) throws Exception {
        post(
                "/applications/" + id + "/status-changes",
                Map.of("version", version(id), "status", status));
    }

    @Test
    void applicationHistoryValidationAndArchive() throws Exception {
        var first = create("Example");
        String id = first.get("id").toString();
        create("Example");
        assertNull(first.get("appliedDate"));
        assertEquals("SAVED", first.get("status"));
        status(id, "INTERVIEWING");
        status(id, "REJECTED");
        status(id, "INTERVIEWING");
        var detail = get(id);
        assertEquals(4, list(detail.get("statusChanges")).size());
        assertNull(list(detail.get("statusChanges")).getFirst().get("eventTime"));
        assertEquals(
                400,
                send(
                                "PATCH",
                                "/api/v1/applications/" + id,
                                Map.of("version", version(id), "expectedAmount", 90000),
                                null)
                        .status());
        assertEquals(
                400,
                send(
                                "PATCH",
                                "/api/v1/applications/" + id,
                                Map.of("version", version(id), "jobUrl", "javascript:alert(1)"),
                                null)
                        .status);
        assertEquals(
                400,
                send(
                                "PATCH",
                                "/api/v1/applications/" + id,
                                Map.of("version", version(id), "unexpected", true),
                                null)
                        .status);
        var edit =
                send(
                        "PATCH",
                        "/api/v1/applications/" + id,
                        Map.of(
                                "version",
                                version(id),
                                "expectedAmount",
                                90000,
                                "expectedCurrency",
                                "czk",
                                "expectedPeriod",
                                "MONTH",
                                "appliedDate",
                                "2026-10-20"),
                        null);
        assertEquals(200, edit.status, edit.text);
        assertEquals("CZK", get(id).get("expectedCurrency"));
        assertEquals(1, matches("90000"));
        assertEquals(1, matches("2026-10-20"));
        assertEquals(
                200,
                send(
                                "PUT",
                                "/api/v1/applications/" + id + "/archive",
                                Map.of("version", version(id), "archived", true),
                                null)
                        .status);
        assertEquals(0, matches("90000"));
        assertEquals(
                200,
                send(
                                "PUT",
                                "/api/v1/applications/" + id + "/archive",
                                Map.of("version", version(id), "archived", false),
                                null)
                        .status);
        assertEquals(1, matches("90000"));
        assertThrows(
                Exception.class,
                () ->
                        jdbc.update(
                                "UPDATE status_changes SET explanation='rewrite' WHERE"
                                        + " application_id=?",
                                UUID.fromString(id)));
    }

    @Test
    void searchAcrossAllRelatedDetailsAndImmediateEdits() throws Exception {
        String id = create("Search example").get("id").toString();
        var type = post("/round-types", Map.of("name", "Architecture discussion"));
        var round =
                child(
                        id,
                        "/rounds",
                        Map.of("typeId", type.get("id"), "feedback", "Neobvyklá zpětná vazba"));
        String rid = round.get("id").toString();
        var note =
                child(
                        id,
                        "/notes",
                        Map.of("roundId", rid, "text", "Dictation about C++ and 3-phase locks"));
        var qa =
                child(
                        id,
                        "/rounds/" + rid + "/questions",
                        Map.of(
                                "direction",
                                "EMPLOYER_ASKED",
                                "question",
                                "Explain consistent hashing under partitions",
                                "answer",
                                "Virtual nodes and bounded redistribution",
                                "sourceNoteId",
                                note.get("id")));
        assertEquals(1, matches("consistent hashing"));
        assertEquals(1, matches("virtual nodes"));
        assertEquals(1, matches("NEOBVYKLÁ"));
        assertEquals(1, matches("C++"));
        assertEquals(1, matches("Architecture discussion"));
        var question = list(list(get(id).get("rounds")).getFirst().get("questions")).getFirst();
        var r =
                send(
                        "PATCH",
                        "/api/v1/applications/"
                                + id
                                + "/rounds/"
                                + rid
                                + "/questions/"
                                + qa.get("id"),
                        Map.of(
                                "version",
                                question.get("version"),
                                "applicationVersion",
                                version(id),
                                "question",
                                "A different question",
                                "answer",
                                "Updated answer"),
                        null);
        assertEquals(200, r.status, r.text);
        assertEquals(0, matches("consistent hashing"));
        assertEquals(1, matches("Updated answer"));
        search.rebuild();
        assertEquals(1, matches("Updated answer"));
        var reason =
                child(
                        id,
                        "/rejection-reasons",
                        Map.of(
                                "typeId",
                                "20000000-0000-0000-0000-000000000004",
                                "label",
                                "Distinct concern",
                                "source",
                                "SELF_ASSESSMENT",
                                "evidence",
                                "Unclear delivery examples"));
        assertEquals(1, matches("delivery examples"));
        status(id, "REJECTED");
        var event = list(get(id).get("statusChanges")).getFirst();
        child(
                id,
                "/rejection-reasons",
                Map.of(
                        "typeId",
                        "20000000-0000-0000-0000-000000000004",
                        "label",
                        "Corrected concern",
                        "source",
                        "SELF_ASSESSMENT",
                        "supersedesId",
                        reason.get("id"),
                        "rejectionEventId",
                        event.get("id")));
        assertEquals(1, matches("Distinct concern"));
    }

    @Test
    void statisticsDoNotMultiplyApplicationsOrMixRejectionEpisodes() throws Exception {
        String id = create("Rejected once").get("id").toString();
        status(id, "REJECTED");
        var values =
                new HashMap<String, Object>(
                        Map.of(
                                "typeId",
                                "20000000-0000-0000-0000-000000000003",
                                "label",
                                "Budget mismatch",
                                "source",
                                "AI_ASSESSMENT",
                                "likelihood",
                                70));
        var first = child(id, "/rejection-reasons", values);
        child(id, "/rejection-reasons", values);
        values.put("source", "COMPANY_FEEDBACK");
        values.remove("likelihood");
        child(id, "/rejection-reasons", values);
        var counts = summary();
        assertEquals(1, ((Number) counts.get("currentlyRejected")).intValue());
        assertEquals(3, ((Number) counts.get("reasonEntries")).intValue());
        assertEquals(
                1,
                ((Number) list(counts.get("reasonCategories")).getFirst().get("applicationCount"))
                        .intValue());
        assertEquals(2, list(counts.get("reasonGroups")).size());
        values.put("source", "AI_ASSESSMENT");
        values.put("supersedesId", first.get("id"));
        child(id, "/rejection-reasons", values);
        assertEquals(3, ((Number) summary().get("reasonEntries")).intValue());
        status(id, "INTERVIEWING");
        assertEquals(0, ((Number) summary().get("currentlyRejected")).intValue());
        assertEquals(1, ((Number) summary().get("everRejected")).intValue());
        status(id, "REJECTED");
        counts = summary();
        assertEquals(2, ((Number) counts.get("rejectionEvents")).intValue());
        assertEquals(0, ((Number) counts.get("reasonEntries")).intValue());
        assertEquals(1, ((Number) counts.get("rejectionsWithoutReason")).intValue());
    }

    @Test
    void simultaneousIdempotentRequestsCreateOneApplicationAndCanonicalRetriesWork()
            throws Exception {
        String key = UUID.randomUUID().toString();
        var input = Map.<String, Object>of("company", "Retry example", "role", "Engineer");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<Reply>>();
            for (int i = 0; i < 4; i++)
                futures.add(
                        executor.submit(() -> send("POST", "/api/v1/applications", input, key)));
            String id = null;
            for (var future : futures) {
                var reply = future.get();
                assertEquals(201, reply.status, reply.text);
                String candidate = json.object(reply.text).get("id").toString();
                if (id == null) id = candidate;
                else assertEquals(id, candidate);
            }
        }
        assertEquals(1, matches("Retry example"));
        var reordered = new LinkedHashMap<String, Object>();
        reordered.put("role", "Engineer");
        reordered.put("company", "Retry example");
        assertEquals(201, send("POST", "/api/v1/applications", reordered, key).status);
        assertEquals(
                409,
                send(
                                "POST",
                                "/api/v1/applications",
                                Map.of("company", "Changed", "role", "Engineer"),
                                key)
                        .status);
        assertEquals(400, send("POST", "/api/v1/applications", input, null).status);
    }

    @Test
    void staleAndCrossApplicationMutationsAreRejectedAndRetryKeepsSearchComplete()
            throws Exception {
        String id = create("Concurrency").get("id").toString(),
                other = create("Other").get("id").toString();
        long initial = version(id);
        var round = child(id, "/rounds", Map.of("typeId", "10000000-0000-0000-0000-000000000003"));
        assertEquals(
                409,
                send(
                                "POST",
                                "/api/v1/applications/" + id + "/notes",
                                Map.of("applicationVersion", initial, "text", "Stale write"),
                                UUID.randomUUID().toString())
                        .status);
        assertEquals(
                404,
                send(
                                "POST",
                                "/api/v1/applications/" + other + "/notes",
                                Map.of(
                                        "applicationVersion",
                                        version(other),
                                        "roundId",
                                        round.get("id"),
                                        "text",
                                        "Wrong owner"),
                                UUID.randomUUID().toString())
                        .status);
        long v = version(id);
        String path = "/api/v1/applications/" + id + "/notes";
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a =
                    executor.submit(
                            () ->
                                    send(
                                            "POST",
                                            path,
                                            Map.of(
                                                    "applicationVersion",
                                                    v,
                                                    "text",
                                                    "Concurrent apple"),
                                            UUID.randomUUID().toString()));
            var b =
                    executor.submit(
                            () ->
                                    send(
                                            "POST",
                                            path,
                                            Map.of(
                                                    "applicationVersion",
                                                    v,
                                                    "text",
                                                    "Concurrent pear"),
                                            UUID.randomUUID().toString()));
            var ra = a.get();
            var rb = b.get();
            assertEquals(Set.of(201, 409), Set.of(ra.status, rb.status));
            child(
                    id,
                    "/notes",
                    Map.of("text", ra.status == 409 ? "Concurrent apple" : "Concurrent pear"));
        }
        assertEquals(1, matches("Concurrent apple"));
        assertEquals(1, matches("Concurrent pear"));
        assertEquals(0, matches("Stale write"));
    }

    private byte[] pdf() throws IOException {
        try (var doc = new PDDocument();
                var bytes = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(bytes);
            return bytes.toByteArray();
        }
    }

    private HttpResponse<byte[]> upload(
            String id, byte[] bytes, String filename, String key, long version) throws Exception {
        String boundary = "JobseekerBoundary";
        var out = new ByteArrayOutputStream();
        out.write(
                ("--"
                                + boundary
                                + "\r\n"
                                + "Content-Disposition: form-data;"
                                + " name=\"applicationVersion\"\r\n\r\n"
                                + version
                                + "\r\n--"
                                + boundary
                                + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                                + filename
                                + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return http.send(
                HttpRequest.newBuilder(
                                URI.create(
                                        base() + "/api/v1/applications/" + id + "/cv-attachments"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .header("Idempotency-Key", key)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    @Test
    void filesAreStoredAsByteaAndUploadsRetryWithoutDuplicateBytes() throws Exception {
        String id = create("Files").get("id").toString(), key = UUID.randomUUID().toString();
        byte[] bytes = pdf();
        long v = version(id);
        var first = upload(id, bytes, "tailored-cv.pdf", key, v);
        assertEquals(201, first.statusCode(), new String(first.body(), StandardCharsets.UTF_8));
        assertEquals(201, upload(id, bytes, "tailored-cv.pdf", key, v).statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM stored_files", Integer.class));
        assertEquals(1, matches("tailored-cv.pdf"));
        var cv = list(get(id).get("cvAttachments")).getFirst();
        var download =
                http.send(
                        HttpRequest.newBuilder(URI.create(base() + cv.get("downloadUrl")))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, download.statusCode());
        assertArrayEquals(bytes, download.body());
        assertEquals(Checksums.sha256(bytes), cv.get("checksum"));
        assertFalse(get(id).toString().contains("content="));
        assertEquals(
                400,
                upload(
                                id,
                                "%PDF-fake".getBytes(StandardCharsets.UTF_8),
                                "broken.pdf",
                                UUID.randomUUID().toString(),
                                version(id))
                        .statusCode());
        assertEquals(
                415,
                upload(
                                id,
                                new byte[] {1, 2, 3},
                                "wrong.exe",
                                UUID.randomUUID().toString(),
                                version(id))
                        .statusCode());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM stored_files", Integer.class));
        assertThrows(
                Exception.class,
                () -> jdbc.update("UPDATE stored_files SET filename='changed.pdf'"));
    }

    @Test
    void failedUploadTransactionLeavesNoFileMetadataSearchOrIdempotencyResult() throws Exception {
        String id = create("Rollback").get("id").toString();
        var file = validator.validate("cv.pdf", pdf());
        long v = version(id);
        assertThrows(
                ApplicationException.class,
                () ->
                        idempotency.execute(
                                "rollback-key",
                                "/test",
                                Map.of("hash", file.checksum()),
                                () -> {
                                    attachments.upload(
                                            UUID.fromString(id),
                                            file,
                                            Map.of("applicationVersion", v),
                                            InputChannel.AUTOMATION);
                                    throw new ApplicationException(
                                            400, "Forced failure after database writes.");
                                }));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM stored_files", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM cv_attachments", Integer.class));
        assertEquals(v, version(id));
        assertEquals(0, matches("cv.pdf"));
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT count(*) FROM idempotency_records WHERE scope LIKE '%rollback-key'",
                        Integer.class));
    }

    @Test
    void docxStructureAndFileSizeAreValidated() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>"
                            .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body/></w:document>"
                            .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertTrue(
                validator
                        .validate("cv.docx", out.toByteArray())
                        .mediaType()
                        .contains("wordprocessingml"));
        assertEquals(
                413,
                assertThrows(
                                ApplicationException.class,
                                () -> validator.validate("cv.pdf", new byte[10485761]))
                        .status());
    }

    @Test
    void browserTemplatesWorkWithoutJavascriptOrAuthenticationAndPreserveFailedInput()
            throws Exception {
        assertEquals(200, send("GET", "/applications", null, null).status);
        var newPage = send("GET", "/applications/new", null, null);
        assertEquals(200, newPage.status, newPage.text);
        String id = create("<script>alert('x')</script>").get("id").toString();
        var round =
                child(
                        id,
                        "/rounds",
                        Map.of(
                                "typeId",
                                "10000000-0000-0000-0000-000000000003",
                                "title",
                                "Technical"));
        child(id, "/notes", Map.of("text", "General dictation"));
        child(id, "/notes", Map.of("roundId", round.get("id"), "text", "Round dictation"));
        child(
                id,
                "/rounds/" + round.get("id") + "/questions",
                Map.of("direction", "CANDIDATE_ASKED", "question", "What is the team like?"));
        status(id, "REJECTED");
        child(
                id,
                "/rejection-reasons",
                Map.of(
                        "typeId",
                        "20000000-0000-0000-0000-000000000001",
                        "label",
                        "Example reason",
                        "source",
                        "AI_ASSESSMENT"));
        var page = send("GET", "/applications/" + id, null, null);
        assertEquals(200, page.status, page.text);
        assertTrue(page.text.contains("&lt;script&gt;"));
        assertFalse(page.text.contains("<script>"));
        assertTrue(page.text.contains("General dictation"));
        assertTrue(page.text.contains("Round dictation"));
        assertTrue(page.text.contains("What is the team like?"));
        var body = "formId=overview&company=&role=Preserve+this+role&version=" + version(id);
        var reply =
                http.send(
                        HttpRequest.newBuilder(URI.create(base() + "/applications/" + id + "/edit"))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(body))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(400, reply.statusCode(), reply.body());
        assertTrue(reply.body().contains("Preserve this role"));
        assertTrue(reply.body().contains("This field is required"));
        long staleVersion = version(id);
        child(id, "/notes", Map.of("text", "A newer saved update"));
        var staleBody =
                "formId=overview&company=Attempted+company&role=Preserve+this+role&version="
                        + staleVersion;
        var conflict =
                http.send(
                        HttpRequest.newBuilder(URI.create(base() + "/applications/" + id + "/edit"))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(staleBody))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(409, conflict.statusCode(), conflict.body());
        assertTrue(conflict.body().contains("Current saved values"));
        assertTrue(conflict.body().contains("Attempted company"));
        assertTrue(conflict.body().contains("&lt;script&gt;"));
        var dashboard = send("GET", "/applications?q=team", null, null);
        assertEquals(200, dashboard.status, dashboard.text);
        assertTrue(dashboard.text.contains("Question / answer"));
    }
}
