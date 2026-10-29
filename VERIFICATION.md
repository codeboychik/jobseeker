# Verification — 2026-10-29

Implemented and checked with Java 21.0.8, Spring Boot 4.1.2, and real PostgreSQL 18.6 databases.

## Automated integration suite

9 tests passed, 0 failures, 0 errors, 0 skipped. Tests run the complete HTTP server and create an isolated database for each suite run.

Covered application history and archive/restore; salary/URL/unknown-field validation; full-detail search and rebuilds; accented case-insensitive text; categorized statistics across repeated rejection episodes; canonical and simultaneous idempotent requests; stale and cross-application mutations; immutable PostgreSQL file bytes; upload retries and transaction rollback; PDF/DOCX validation and size limits; escaped HTML; and failed browser form input preservation.

The final Maven-wrapper `verify` run passed all tests and packaged the executable JAR successfully.

## Browser check

Using the packaged app, created an example application through the UI, added a technical interview and a question/answer, recorded an interviewing status, then found the application by searching for words present only in that question. The result linked directly to the matching round. Checked the CV metadata and original download link after a subsequent upload.

Preview screenshots contain only fictional verification records:

- [Dashboard](artifacts/dashboard.jpg)
- [Question search](artifacts/dashboard-search.jpg)

## Transfer/recovery check

1. Prepared two fictional applications, interview Q&A, a categorized rejection, and a PDF stored as PostgreSQL `bytea`.
2. Created a custom-format database backup with `pg_dump` 18.1.
3. Initialized a second independent PostgreSQL instance and restored the backup using `pg_restore`.
4. Started a fresh app process against that restored database, without copying an application file directory.
5. Compared application detail, Q&A, search results, and the complete statistics summary; all matched.
6. Downloaded the restored original PDF and verified its SHA-256 checksum matched the source exactly.

## Docker deployment and container persistence

- `docker compose config --quiet` passed.
- Built the multi-stage `jobseeker:0.1.0` image and started the Spring Boot/PostgreSQL stack with Compose.
- Confirmed the containerized dashboard returns HTTP 200, the app runs with UID 10001, and PostgreSQL's file-content column is `bytea`.
- Reran all 9 integration tests against Docker's PostgreSQL service: 0 failures, 0 errors, 0 skipped. The suite used its own random database.
- Started an isolated `jobseeker-check` Compose project with separate ports and a separate PostgreSQL volume; populated only fictional applications, Q&A, a categorized rejection, and an example PDF.
- Replaced both containers with `--force-recreate`, then compared application detail, statistics, full-text search results, and the original PDF checksum. All matched.
- Removed the isolated verification project and its test volume. Temporary native verification services were also stopped.

The main `jobseeker` Compose stack remains running at [http://localhost:8080/applications](http://localhost:8080/applications). Its database contains no example applications. Both services use `restart: unless-stopped`; only PostgreSQL holds persistent business data. Run `docker compose down` to stop the stack while preserving its database volume.

## Code organization refactor

The implementation is now organized by feature, with focused services/repositories, resource-specific API controllers, explicit browser form controllers, shared validation rules, and separate PDF/DOCX validators. The database schema and external routes/JSON fields are unchanged. Templates moved to `templates/applications/`, with a separate reusable form-fragment file.

After restructuring, `spotless:apply verify` passed all 9 PostgreSQL integration tests, the Java formatting check, and executable-JAR packaging. The suite covers rendering and form preservation as well as history, search, statistics, uploads, idempotency, and concurrency. See [the architecture guide](docs/architecture.md) for package responsibilities and transaction boundaries.
