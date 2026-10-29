# Jobseeker

A personal Spring Boot application with two server-rendered pages: an application dashboard and an editable application detail. PostgreSQL stores everything, including original CV bytes. No login, authorization, API token, frontend build, or persistent application file directory is required.

## Run with Docker

Docker Compose is the primary run path. Only Docker Desktop (or Docker Engine with Compose) is needed; Java, Maven, and PostgreSQL do not need to be installed on the host.

```sh
docker compose up --build -d
```

Open [http://localhost:8080/applications](http://localhost:8080/applications). The multi-stage build produces the `jobseeker:0.1.0` image and runs it as a non-root user. PostgreSQL uses a named volume; replacing either container preserves all records and CV files. Both services restart automatically unless explicitly stopped. `docker compose down` stops the stack while retaining the database volume.

Copy `.env.example` to `.env` to customize `APP_PORT`, `APP_BIND`, `DB_PORT`, and `DB_PASSWORD`. Defaults bind the published ports to localhost. The app can also connect directly to any configured PostgreSQL service.

```sh
docker compose logs -f app
docker compose down
```

To move the built application image without rebuilding, use `docker save -o jobseeker-image.tar jobseeker:0.1.0` and `docker load -i jobseeker-image.tar` on the other device. Transfer the PostgreSQL backup and connection configuration as described below; the app container has no persistent file volume.

## Run from source

Requirements: JDK 21 or newer, PostgreSQL 18, and `JAVA_HOME` configured. The Maven wrapper downloads Maven 3.9.11. A separate Maven installation is unnecessary.

```sh
docker compose up -d db
./mvnw spring-boot:run
```

On Windows use `./mvnw.cmd`. To build an executable JAR:

```sh
./mvnw clean package -DskipTests
java -jar target/jobseeker-0.1.0.jar
```

Configuration is read from environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobseeker` | JDBC connection; database must already exist. |
| `DB_USER`, `DB_PASSWORD` | `jobseeker` | PostgreSQL credentials. |
| `PORT`, `BIND_ADDRESS` | `8080`, `0.0.0.0` | HTTP server settings. Compose publishes on localhost by default. |
| `APP_TIMEZONE` | `Europe/Prague` | Display and browser-form input timezone. API timestamps require an offset. |
| `UPLOAD_LIMIT_BYTES` | `10485760` | Validated maximum CV size. |
| `MAX_FILE_SIZE`, `MAX_REQUEST_SIZE` | `10MB`, `11MB` | Servlet upload limits; adjust together with the byte limit. |

Flyway applies versioned schema migrations on startup. The implementation uses Spring JDBC to make PostgreSQL binary storage, full-text search, locking, and aggregation queries explicit.

## Working with applications

- Create with company and role only. Unknown dates remain unknown; recording an application is distinct from confirming submission.
- Save exact PDF/DOCX copies, mark them prepared or sent, and download any original. New uploads preserve older files. CV metadata is fetched separately from binary bytes.
- Add predefined or custom rounds, employer/candidate questions, unanswered questions, free-form notes, and pasted dictation. Edit sections independently; display-order numbers control round/Q&A ordering.
- Record status changes, historical dates, and corrections. Status history is append-only. Reopening and archiving preserve past outcomes.
- Add categorized company feedback, self-assessments, and AI hypotheses. Corrections append a replacement assessment. Probabilities are optional estimates; no AI provider is called by the app.
- Every edit checks the last-read version. A stale form shows the current saved record and preserves the attempted input for review.

Search covers all saved detail fields, Q&A, notes, reason history, dates, salary values, and CV metadata. PostgreSQL uses a generated `tsvector`, a GIN index, and a complementary literal match for names such as `C++`, dates, and URL fragments. The service normalizes Unicode case independently of the database's locale. Multiple words mean all words may occur across the application's details; literal substring matches also qualify. Search is updated in the same transaction as every mutation. PDF/DOCX contents, OCR, typo correction, and semantic search are not included.

## Automation API

The API is at `/api/v1`. The complete [OpenAPI document](src/main/resources/static/api-docs.json) is also served at [http://localhost:8080/api-docs.json](http://localhost:8080/api-docs.json).

For example:

```sh
curl -X POST http://localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 39558927-1ddd-4d44-a980-bb867086ebad' \
  -d '{"company":"Example","role":"Backend engineer","description":"Java and PostgreSQL"}'
```

On Windows use `curl.exe` or `Invoke-RestMethod` to avoid PowerShell's curl alias.

All POST operations require a unique `Idempotency-Key`. Retrying the same route, key, and payload returns the original successful response, even after a version changes. A different payload with the same key returns `409`. Failed requests do not reserve their key. Idempotency records commit atomically with the business change; simultaneous duplicates serialize safely.

Application edits, status changes, and archive actions require `version`. Child creation requires `applicationVersion`; child edits/deletion require both `applicationVersion` and the child's `version`. Child create responses include `id`, `applicationVersion`, and the updated `application`. PATCH omits unchanged fields and uses explicit null to clear optional fields; unknown keys are rejected. Use GET application detail to retrieve all current versions.

```sh
curl -X POST http://localhost:8080/api/v1/applications/APPLICATION_ID/notes \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: ANOTHER_UNIQUE_KEY' \
  -d '{"applicationVersion":0,"text":"Paste the interview dictation here."}'

curl 'http://localhost:8080/api/v1/applications?q=consistent%20hashing&archive=all'
curl 'http://localhost:8080/api/v1/statistics/summary?archive=all'
```

Upload with multipart form fields `file`, `applicationVersion`, optional `usage` (`PREPARED`/`SENT`), and optional `sentDate`:

```sh
curl -X POST http://localhost:8080/api/v1/applications/APPLICATION_ID/cv-attachments \
  -H 'Idempotency-Key: ANOTHER_UNIQUE_KEY' \
  -F 'applicationVersion=1' -F 'usage=PREPARED' -F 'file=@tailored-cv.pdf'
```

An application status change uses `status`, `version`, and optional `eventTime`/`explanation`. Rejection reasons use `typeId` from `/rejection-reason-types`. For a currently rejected application, an omitted `rejectionEventId` selects its latest rejection event; explicit null leaves an unassigned concern. `supersedesId` appends a correction. Evidence can reference a round, question, and/or note belonging to the same application.

`GET /statistics/summary` accepts the same `q`, `status`, and `archive=active|archived|all` filters as the list. It distinguishes current rejections, applications ever rejected, rejection events, active reason entries, and distinct applications by stable reason category/source. Superseded reasons and unassigned concerns do not inflate current statistics. Unknown reasons have their own count. Category totals may overlap and are not causal percentages.

After a direct database import, rebuild search with an idempotent `POST /api/v1/search/rebuild` and JSON body `{}`. Normal UI/API edits need no rebuild.

## Source structure

Code is grouped by feature, with focused services/repositories and separate JSON and browser controllers. Shared validation, JDBC, JSON, and audit support have dedicated packages. The two application templates live under `templates/applications`, with reusable form fragments alongside them. See [the architecture guide](docs/architecture.md) for responsibilities and transaction boundaries.

Run `./mvnw spotless:apply` to format Java. `./mvnw verify` also checks formatting. Use `mvnw.cmd` on Windows.

## Tests

The integration suite starts the complete HTTP server against a real PostgreSQL database, not H2 or mocks. Start a disposable test server:

```sh
docker run -d --name jobseeker-tests -p 127.0.0.1:55432:5432 \
  -e POSTGRES_USER=jobseeker -e POSTGRES_PASSWORD=jobseeker postgres:18
./mvnw verify
docker rm -f jobseeker-tests
```

Wait for PostgreSQL to be ready before running Maven. Alternatively set `TEST_PG_URL` (an administrative JDBC URL ending in `/postgres`), `TEST_PG_USER`, and `TEST_PG_PASSWORD`. The account must be able to create databases. Every test run creates and drops its own random `jobseeker_test_*` database; it never truncates a user database. The suite fails clearly if PostgreSQL is unavailable.

Coverage includes history, money/URL validation, Unicode and related-record search, search rebuilds, stale/cross-application writes, concurrent API retries, categorized statistics across rejection episodes, immutable binary files, upload transaction rollback, PDF/DOCX validation, and JavaScript-free HTML workflows.

## Backup, restore, and moving devices

One consistent PostgreSQL backup contains records, CV bytes, schema, search projections, and idempotency data. Use `pg_dump` from the same major PostgreSQL version or a newer compatible client. There is no application-local file directory to copy.

With Compose, use a file inside the container and `docker compose cp`; this avoids binary redirection problems in Windows PowerShell:

```sh
docker compose exec -T db pg_dump -U jobseeker -d jobseeker -Fc -f /tmp/jobseeker.backup
docker compose cp db:/tmp/jobseeker.backup ./jobseeker.backup
```

Restore into a new empty database on the target PostgreSQL service:

```sh
docker compose exec -T db createdb -U jobseeker jobseeker_restored
docker compose cp ./jobseeker.backup db:/tmp/jobseeker.backup
docker compose exec -T db pg_restore -U jobseeker --no-owner --no-privileges -d jobseeker_restored /tmp/jobseeker.backup
```

Start the app with `DB_URL` pointing at the restored database, then check one application, a search term, statistics, and a CV download checksum. For a clean installation on a different device, deploy the JAR/container and PostgreSQL, restore the database, and transfer the connection configuration. Future schema updates remain managed by Flyway.

Scheduled job discovery, CV generation, employer submissions, built-in dictation/AI calls, and charting are future work, as agreed in [MVP_SPEC.md](MVP_SPEC.md).
