# Jobseeker — MVP specification

Date: 2026-10-29  
Status: Implemented in this workspace. See README.md for running, API use, and verification.

## 1. Purpose and evaluation

Build a personal job application tracker with two main pages: a dashboard and an application detail. It must retain past applications, their status changes, the exact CV files used, interview rounds, questions and answers, salary expectations, and outcomes.

The proposed workflow is coherent. The tracker should be the durable record of the job search, with both browser forms and an API writing to the same underlying services. This makes manual entry useful immediately and allows a later ChatGPT integration to populate the same records.

The following decisions define the initial scope:

1. One application represents one attempt at one role. Company name alone is insufficient: there may be several roles or repeated applications at the same company.
2. Keep a current status and an append-only history of status changes. Updating a dashboard cell must not erase the previous state.
3. Store uploaded CV copies as immutable binary records in PostgreSQL, referenced by application attachments. A database backup transfers the complete application data, including files, across devices.
4. Distinguish company feedback from suspected rejection reasons. An AI estimate can be useful for reflection, but a percentage is not a verified explanation or a calibrated probability.
5. Search the complete saved application detail from the dashboard through a PostgreSQL full-text index, including related interview and Q&A records.
6. Store outcomes and reusable rejection-reason categories as structured data so statistics can be added to the existing dashboard.
7. Provide the personal-use UI, downloads, and API without authentication or authorization in the MVP.

The MVP includes storage for AI assessments and an API through which an external tool can supply them. Scheduled job discovery, CV generation, AI inference inside the application, and submission to employers are later capabilities. None is required to record an application or interview.

## 2. Assumptions and scope boundary

Confirmed requirements from the user's scope revision:

- PostgreSQL stores both business records and file bytes. No persistent application-local file directory or separate object-storage service is required.
- Dashboard search covers information throughout application details, including child records.
- Data must support application/outcome counts and meaningful rejection-reason statistics.
- No authentication or authorization is required for this personal-use app.

Other proposed defaults:

- One owner; English interface; a personal deployment that can move between devices or connect to an externally hosted PostgreSQL instance.
- One Spring Boot application and one PostgreSQL database containing all persistent data, including CV files. The application has no persistent local state to transfer.
- Exactly two business page templates. Creation and editing reuse the application detail template; no login page is needed.
- Company and role title are required. All information that may be unknown initially, including dates, CV, description, and salary, can be added later.
- A draft record is permitted before an application has been submitted. It begins in `SAVED` status.
- UI default timezone: `Europe/Prague`. Currency and salary period are chosen explicitly rather than inferred.
- Archive/unarchive is available. Permanent deletion, multiple users, and shared team workflows are outside the MVP.

### Included in MVP

- Create, view, edit, search, filter, and archive application records.
- Record current status, status history, final outcome, and company feedback.
- Upload and download exact CV copies; distinguish prepared CVs from sent CVs.
- Add any number of interview rounds, including repeat and custom round types.
- Record employer questions and the candidate's answers, candidate questions and employer answers, and free-form notes.
- Record salary expectation and, optionally, an offer using the same simple money structure.
- Record multiple possible rejection reasons with optional probability estimates and their sources.
- Search all saved detail text and displayed scalar values from the dashboard using PostgreSQL full-text search, with immediate updates after changes.
- Use reusable reason categories and provide basic statistics queries/API data for later dashboard integration.
- Provide a documented JSON API for equivalent data input, without authentication or authorization.
- Persist database records and file bytes together across restarts; document a single-database backup and restore.

### Deferred

- Searching job boards or career pages, matching jobs, and scheduled searches.
- Generating or rewriting CVs; a master CV library; tracking versions before they are attached to an application.
- Submitting applications, sending emails, or handling employer website sessions.
- Built-in chat, microphone recording, speech-to-text, or automated extraction of structured Q&A from notes.
- Calling an AI model from the application to analyze interviews or rejection reasons.
- Email/calendar integrations, notifications, a dedicated statistics page or charts, multi-user access, and hosting automation.
- Authentication, authorization, accounts, API credentials, and role management.
- Extracting text from CV file contents or OCR; file names and other saved file metadata are searchable in the MVP.

## 3. Dashboard

Route: `GET /applications`; `/` redirects here. Template: `applications/dashboard.html`.

One row represents one application, with these columns:

| Column | Behavior |
| --- | --- |
| Company | Required company name. |
| Role | Required title; distinguishes applications at the same company. |
| Job posted | Optional posting date; show “Unknown” when unavailable. |
| Applied | Optional actual submission date; blank means not recorded, not automatically “not applied.” |
| Current status | Human-readable status label. |
| Detail | Link to this application's detail page. |

Controls:

- **New application** opens the detail template in creation mode.
- Search all application details, including interview questions and answers, as specified below; filter by current status and archive state.
- Sort by company, role, posting date, application date, or status. Without a search query, default to most recently updated first, with ID as a stable tie-breaker. With a query, default to relevance, then updated time and ID; an explicit sort overrides relevance ordering.
- Server-side pagination, default 25 rows. Unknown dates sort after known dates.
- Archived records are hidden by default and remain searchable through the archive filter.
- Preserve filters and page position when returning from a detail.
- Show a clear empty state and an action to add the first application.

No charts, animations, or draggable boards are needed.

### 3.1 Search behavior and implementation

The dashboard's single search box finds an application even when the matching information appears only inside its detail. Return each matching application once, irrespective of how many rounds or answers match, and show a short escaped excerpt identifying the matching section. Link the result to that section or round on the detail page.

Search scope:

- Company, role, job URL/reference, role description, company feedback, and general notes.
- Current and historical status names, status-change explanations, dates/times, and displayed source labels.
- Salary amounts, currencies, periods, and basis; posting/application dates and other displayed scalar metadata use canonical text representations, including ISO dates and decimal numbers.
- Round types/titles, round status/result, feedback, and round notes.
- Employer and candidate questions, answers, and Q&A notes, including unanswered questions.
- Rejection-reason categories, labels, explanations, evidence, source, likelihood values, and retained superseded assessments visible in history.
- CV filenames, format, size/checksum, prepared/sent labels, and sent/upload dates. Raw PDF/DOCX bytes are not text fields; document-content extraction and OCR are deferred explicitly.

PostgreSQL design:

1. Maintain one derived search row per application with `application_id` as its primary/foreign key, `search_document text`, and a stored `search_vector tsvector` generated from that document. Add a GIN index on the vector. No external search service is needed.
2. Build the document by aggregating the application's own fields and all related searchable records in stable order, with section separators and null-safe concatenation. Keep the relational tables as the source of truth; the search row is a rebuildable projection, never the statistics source.
3. Use an explicit `simple` text-search configuration initially so technical vocabulary and mixed-language notes are not subjected to English-only stemming or stop-word removal. Parse normal queries with `plainto_tsquery` using the same configuration; multiple words mean all words must occur somewhere in the application's searchable detail. Search is case-insensitive; typo tolerance, stemming, and semantic search are outside this MVP.
4. Include a case-insensitive literal-substring match against the same search document to find exact identifiers, punctuation-bearing technical names, URL fragments, dates, and amounts that tokenization might not preserve. Bind parameters and escape literal `%`/`_`. Rank literal matches ahead of ordinary full-text matches, then apply full-text relevance. This complementary path can scan the small initial dataset; a trigram index can be added if measured performance calls for it.
5. Refresh the search document in the same transaction as every applicable create/update/delete/supersede operation, from both forms and API. Acquire the parent application lock before a related mutation and refresh after flushing it so concurrent child edits cannot leave a stale projection. Rollback also rolls back the search change. Do not use an asynchronous job or a materialized view requiring manual refresh.
6. A generated column cannot aggregate child tables directly. The service assembles `search_document`; the generated vector only depends on that row's document. Provide a rebuild operation and migration backfill for existing records. Any future direct database import must perform the same refresh/rebuild before its results are considered ready.
7. Empty/whitespace-only queries show the ordinary dashboard. Apply current-status/archive filters consistently to search, result totals, pagination, and summary statistics. Historical text remains searchable even when the current status differs.

Examples: searching for a technical question's distinctive words finds its application even when those words appear nowhere in the company or role; searching for a sentence in interview notes or a rejection explanation works the same way. Editing/removing a Q&A entry updates the results immediately. History that is deliberately retained remains searchable.

## 4. Application detail

Routes: `GET /applications/new` and `GET /applications/{id}`. Template: `applications/detail.html`, with reusable form fragments in `applications/fragments/forms.html`.

Use a single readable page with sections and ordinary forms. Each existing section saves independently so an interview note does not require resubmitting the entire application.

### 4.1 Application and role

| Field | Required | Notes |
| --- | --- | --- |
| Company name | Yes | Free text; no separate company management screen. |
| Role title | Yes | Free text. |
| Job posting URL | No | Original career page or job board URL. |
| External job reference | No | Employer or job board identifier when known. |
| Posting date | No | Date only. |
| Applied date | No | Date only; never inferred from record creation time. |
| Role description | No | Saved plain-text snapshot, independent of the external page. |
| Current status | Yes | Defaults to `SAVED`; subsequent changes use the status action. |
| Expected salary | No | Money fields described below. |
| Offered salary | No | Same structure; independent of expectation. |
| Company feedback | No | What the company actually communicated, in the user's words or copied text. |
| General notes | No | Free text. |

Salary is a decimal number, never floating point, plus currency and period (`HOUR`, `MONTH`, or `YEAR`). When an amount is present, currency and period are required. An optional basis (`GROSS`, `NET`, `UNSPECIFIED`) removes ambiguity. Example: `90000 CZK / MONTH, GROSS`. Do not convert currencies or periods in the MVP. A missing amount is unknown, not zero; reject zero or negative amounts.

### 4.2 CV attachments

- Upload PDF or DOCX files, up to 10 MiB per file by default; make the limit configurable.
- Store original bytes in `StoredFile.content bytea`, together with original filename, detected format, byte size, SHA-256 checksum, and upload timestamp. `CvAttachment` links an application to that file through a foreign key and holds application-specific usage metadata. These are ordinary related PostgreSQL tables.
- Use `bytea` directly, not Base64 text or PostgreSQL large-object OIDs. Given the expected small file count and size, keep this in the existing PostgreSQL service; no filesystem mount, object store, or additional deployment service is required.
- Each attachment has usage `PREPARED` or `SENT` and an optional sent date. Marking a CV as sent is an explicit action; uploading it does not mean it was submitted.
- An application can have several attachments, including more than one sent CV if the company requests an updated copy. Show all of them with their usage labels.
- File bytes are immutable. Corrections use a new upload; keep the older attachment. Usage metadata can be corrected separately.
- Do not require an attachment or a known sent date to save an application. Historical applications may have missing files.
- Downloads return the original bytes and a safe filename, by application/attachment ID, without a login or token. Dashboard, detail, and attachment-list queries select metadata without selecting file bytes; only the download operation retrieves the binary content.
- Insert file bytes and attachment metadata atomically in one database transaction. A failed upload leaves neither a partial attachment nor an orphan file row. A database backup/restore carries both metadata and bytes to another device or PostgreSQL instance.
- Validate size and content structure appropriate to PDF/DOCX, not just the filename extension. Reject other formats and malformed files with an understandable error.
- Never fetch a remote file automatically just because a job description or API request contains a URL.

### 4.3 Interview rounds

Each round contains:

- Type, optional title, and display order.
- Optional scheduled date/time and optional completed date/time.
- Round status: `PLANNED`, `COMPLETED`, or `CANCELLED`.
- Round result: `PENDING`, `PASSED`, `FAILED`, or `NOT_PROVIDED`.
- Optional feedback and free-form notes, including a pasted dictation transcript.
- Zero or more question/answer entries.

Predefined types: pre-screen, recruiter interview, technical interview, homework/take-home, behavioral interview, team fit, and cultural fit.

The type selector includes **Add custom type** with a text field. New types become reusable in future rounds and applications. Ignore surrounding whitespace and prevent case-insensitive duplicate names. Existing type names are immutable in the MVP; a different name is a new type. This prevents later renaming from changing historical descriptions.

Allow several rounds of the same type, custom ordering, and adding a round after the process has ended to reconstruct past interviews. Cancelling a round preserves its contents. A round result does not automatically accept or reject the whole application.

### 4.4 Questions, answers, and dictated notes

Each entry belongs to a specific round and has:

- Direction: `EMPLOYER_ASKED` or `CANDIDATE_ASKED`.
- Question text, optional answer text, optional notes, and display order.

The direction determines who provided the answer. An unanswered question is valid and remains visibly unanswered. Preserve multiline text. Entries can be added, edited, reordered, or removed independently.

For quick entry, provide a large plain-text notes field on every round and at application level. The user can paste text dictated in ChatGPT or another tool. Structured Q&A may be entered later manually or through the API. Do not invent answers or overwrite the source note during structuring. An automated client can reference the originating note ID when creating a Q&A entry.

The MVP stores text supplied by an external chat; it does not listen to or access the user's chats itself.

### 4.5 Status history and outcome

| Status | Meaning |
| --- | --- |
| `SAVED` | Opportunity recorded; submission not yet confirmed. |
| `APPLIED` | Application submitted. |
| `INTERVIEWING` | Screening, interviews, or assessment underway. |
| `OFFERED` | An offer was received; no acceptance recorded. |
| `ACCEPTED` | The candidate accepted an offer. |
| `REJECTED` | The employer rejected the application. |
| `WITHDRAWN` | The candidate ended the process. |
| `CLOSED` | Process ended without a recorded acceptance, rejection, or withdrawal. |

Display “Offer received” separately from “Offer accepted” so an employer's positive decision and the candidate's decision remain distinguishable.

- The result section is derived from current status; do not store a second conflicting accepted/rejected flag.
- Every actual status change appends an event containing a monotonic per-application sequence, previous status, new status, business event time, recorded time, optional explanation, and source (`MANUAL` or `AUTOMATION`) with an optional descriptive actor label.
- Creating a record writes an initial event, with no previous status. Historical creation can specify the current known status; unknown earlier transitions are not fabricated.
- Recorded time is always known; business event time may be null when reconstructing history with no known date. Display it as unknown and do not substitute the import/recording time when calculating outcome-date statistics.
- Updating current status and adding its event occur in one database transaction. The last committed status change defines current status; backdating the event time does not silently reorder the state machine.
- Allow direct jumps and explicit reopening/correction, since old applications may be entered retrospectively. Preserve the old events and explain corrections in a new event.
- Repeating the same status does not add another status change. Additional commentary goes into notes.
- An applied date may remain unknown even for an application in `INTERVIEWING` or `REJECTED`. Changing status must not silently fill it with today's date.
- Show history with both effective event time and recorded time where they differ.
- Archive/unarchive is separate from status and does not change the outcome or history.

### 4.6 Rejection reasons

Allow zero or more reasons. An unknown reason is valid; rejecting an application does not require speculation.

Each reason contains:

- A reusable reason-category reference with a stable ID/code, separate from the free-form label and explanation. Seed categories such as technical skills, experience, compensation mismatch, communication, role/team fit, hiring change, and other. Allow adding a custom category inline and reusing it; prevent case-insensitive duplicates and preserve existing category identity.
- Short label and optional explanation.
- Source: `COMPANY_FEEDBACK`, `SELF_ASSESSMENT`, or `AI_ASSESSMENT`.
- Optional estimated likelihood from 0 through 100, inclusive; absent means not assessed.
- Optional supporting evidence text and references to relevant round, Q&A, or note IDs belonging to the same application.
- Assessed timestamp and optional assessor/model identifier when supplied by an external tool.
- An optional reference to the specific rejection status-change event it describes. Validate that the event belongs to this application and changes its status to `REJECTED`. When entering a reason for a currently rejected application, default to its current rejection event; concerns entered before rejection remain unassigned until explicitly linked.

Show company feedback separately from assessments. Label percentages “Estimated likelihood” and AI reasons “AI hypothesis.” An assessment without evidence must say so. The percentages do not need to add up to 100: several contributing reasons can overlap. Do not assign 100 automatically to company feedback or assign a default percentage when none was supplied.

Keep previously supplied assessments when new ones arrive; correction can mark an assessment superseded and retain the original. Retain the latest company feedback separately from the hypothesis list. Reasons may be stored before rejection, but the UI presents them as possible concerns until the application is actually rejected.

The category provides a reliable grouping key for future statistics; the free-form text preserves the particular explanation. A reason about compensation can have its own wording without creating a new statistical category. Multiple assessments of one category may coexist, but statistical counts distinguish reasons from distinct applications affected.

## 5. Minimal data model

Use relational tables with explicit foreign keys. Company names remain snapshots on applications; a shared company directory is unnecessary at this stage.

| Entity | Principal data and relationships |
| --- | --- |
| `Application` | UUID; company; role; job URL/reference; posting/applied dates; description; current status; expected/offered salary fields; company feedback; archive flag; created/updated timestamps; optimistic-lock version. |
| `StatusChange` | UUID; application; unique per-application sequence; previous/new status; optional business event time; required recorded time; source; actor; explanation. Append-only; sequence defines the latest committed event. |
| `StoredFile` | UUID; immutable `content bytea`; original filename; format; size; SHA-256 checksum; uploaded timestamp. One file row per upload; deduplication is unnecessary for the MVP. |
| `CvAttachment` | UUID; application foreign key; unique stored-file foreign key; usage; sent date; created/updated timestamps; version for metadata edits. File bytes are not part of an application/detail query. |
| `RoundType` | UUID; unique normalized name; display name; predefined/custom marker. |
| `InterviewRound` | UUID; application; round type; title/order; scheduled/completed times; status; result; feedback; created/updated timestamps; version. |
| `QuestionAnswer` | UUID; round; direction; question; answer; notes; order; optional source-note reference; created/updated timestamps; version. |
| `Note` | UUID; application; optional round; text; source/actor; created/updated timestamps; version. Application and round notes use the same structure. |
| `RejectionReasonType` | UUID; stable unique code; unique normalized name; display name; predefined/custom marker. Reusable statistical grouping key; preserve its historical identity. |
| `RejectionReason` | UUID; application; reason-type foreign key; optional rejection-event foreign key; label; explanation; source; optional likelihood/evidence/model; evidence references; assessed/recorded times; optional superseding-reason reference. |
| `ApplicationSearch` | Application UUID primary/foreign key; aggregated `search_document text`; stored generated `search_vector tsvector`; GIN index; indexed-document version. Rebuildable derived data. |
| `IdempotencyRecord` | Method/route/key identity; request hash; stored successful response body/status and resource identity; completion time. Supports safe API retries without client authentication. |

All mutable business records retain input channel (`MANUAL` or `AUTOMATION`) and an optional supplied actor/client label as metadata. With no authentication, labels are descriptive and are not verified identities. Status history and superseded reasons preserve their earlier values; full revision history for every text edit is outside MVP scope.

Rules:

- Store calendar dates as dates and actual timestamps as UTC instants. Display times in the configured timezone. Unknown values remain null.
- Use decimal money and a database constraint for likelihood bounds. Validate required values on both input paths.
- Check that nested resources and evidence references belong to the addressed application; do not trust IDs supplied by a client.
- Identical company/title combinations are allowed. Warn about a likely duplicate matching job reference or posting URL; do not merge records automatically.
- Add indexes supporting current status/archive filtering, status event type/time, reason category/source/event, recent updates, foreign-key lookups, and full-text search. Keep large binary values out of search and statistics query projections.

### 5.1 Statistics foundation

Keep structured status, event time, reason category, source, and likelihood as typed columns with foreign keys. Do not derive statistics by parsing notes, matching free-form reason labels, or reading the full-text document. The existing dashboard can later display statistics by calling a small summary service/API; no new page or chart is needed in this MVP.

Provide SQL-backed aggregate queries and a summary response with explicit definitions:

| Metric | Definition |
| --- | --- |
| Application count | Distinct application IDs within the requested search/status/archive filters. |
| Counts by current status | One bucket per status, using the same filtered application set. Each application contributes once. |
| Currently rejected applications | Distinct applications whose current status is `REJECTED`. Reopened applications are excluded. |
| Applications ever rejected | Distinct applications with at least one recorded `REJECTED` status event, within the same filtered application set. |
| Rejection events | Number of actual `REJECTED` events, separately named; repeated rejection after reopening can create more than one event per application. |
| Current rejection-reason entries | Count non-superseded reason rows linked to the current rejection event of currently rejected applications. Report separately by source. Unassigned pre-rejection concerns are excluded. |
| Applications by rejection category | Group by stable reason-type ID/code and source; count distinct application IDs using those same current rejection reasons. Multiple descriptions/model assessments cannot inflate an application's count within a category/source. |
| Rejections without a recorded reason | Currently rejected applications with no non-superseded reason linked to their current rejection event. Unknown reasons remain visible in statistics rather than disappearing. |

Additional rules:

- Apply search/status/archive filters to the application set first, then aggregate child data. Do not join several one-to-many tables and count multiplied rows. Default statistics exclude archived applications, consistently with the list; an explicit archive filter can include them.
- Keep reported company feedback, self-assessments, and AI hypotheses in separate source groups. A reason-category total across all sources also deduplicates application IDs rather than summing source-group counts.
- If the user records an actual company explanation as feedback text only, it remains searchable but is not silently classified into a reason category. The detail form can explicitly add a categorized company-feedback reason referencing that explanation.
- Superseded reasons stay available in history/search but are excluded from current reason counts. A reopened and subsequently rejected application uses the new rejection event; old reasons are retained and can be explicitly reused in new assessments.
- Counts across categories can exceed the number of rejected applications because causes may overlap. Never sum estimated likelihoods into a rejection count or present them as established causal rates.
- Preserve dates and events for future monthly/cohort/funnel/time-to-outcome statistics. Unknown business dates stay unknown and must not silently use record creation date. A future rate must state its denominator and time basis explicitly.
- All aggregates are calculated from the current relational data, without cached counters that can drift after edits. A future statistics dashboard can reuse the summary service and add grouping queries without changing the application/reason model.

## 6. Automation API

Base path: `/api/v1`. Browser and API controllers use the same services, transactions, and validation. The API supports the MVP data; there is no requirement for browser automation to populate forms.

| Resource | Operations |
| --- | --- |
| `/applications` | `GET` paginated search/filter/sort; `POST` create. |
| `/applications/{id}` | `GET` complete detail; `PATCH` application fields, excluding current status and archive flag. |
| `/applications/{id}/status-changes` | `GET` history; `POST` explicit status change. |
| `/applications/{id}/archive` | `PUT` set `archived` true/false. |
| `/applications/{id}/cv-attachments` | `GET` metadata list; multipart `POST` upload. |
| `/applications/{id}/cv-attachments/{attachmentId}` | `PATCH` usage/sent-date metadata. |
| `/applications/{id}/cv-attachments/{attachmentId}/download` | `GET` original file bytes. |
| `/round-types` | `GET` available types; `POST` custom type. |
| `/rejection-reason-types` | `GET` available reason categories; `POST` custom category. |
| `/applications/{id}/rounds` | `GET` list; `POST` create. |
| `/applications/{id}/rounds/{roundId}` | `PATCH` edit fields or display order. |
| `/applications/{id}/rounds/{roundId}/questions` | `GET` list; `POST` add. |
| `/applications/{id}/rounds/{roundId}/questions/{questionId}` | `PATCH` edit/order; `DELETE` remove an erroneous entry. |
| `/applications/{id}/notes` | `GET` list; `POST` add, with optional round reference. |
| `/applications/{id}/notes/{noteId}` | `PATCH` correct text. |
| `/applications/{id}/rejection-reasons` | `GET` list; `POST` add an assessment or append a correction that supersedes an earlier reason. |
| `/statistics/summary` | `GET` application/status/rejection/reason counts with the dashboard's same `q`, status, and archive filters. Return metric definitions, reason IDs/codes, sources, entry counts, and distinct-application counts; a chart or additional page is not required. |

Contract requirements:

- JSON request/response schemas and an OpenAPI document describe nullable fields, enums, salary structure, full-detail search, statistics definitions, pagination, and upload forms. The OpenAPI file may be maintained explicitly; runtime API-documentation dependencies are optional.
- Dates use ISO `YYYY-MM-DD`; timestamps include an offset and normalize to UTC. IDs are stable UUIDs. Detail responses include nested records, source metadata, current versions, and attachment download links, but no server filesystem paths.
- `PATCH` omits fields that should remain unchanged; explicit null clears an optional field. Unknown/unsupported fields produce a validation error rather than being silently ignored.
- Use `201` plus resource identity for creation; `400` with field errors for invalid input; `404` for missing resources; `409` for version or idempotency conflicts; `413` for oversized uploads; `415` for unsupported upload formats. No login, token, or authorization-related response is part of the MVP contract.
- Require an `Idempotency-Key` for `POST` operations. Scope keys by HTTP method and canonical route; use a globally unique key such as a UUID. No authenticated client identity is required. The same key and same payload return the original successful result; a changed payload returns `409`. Persist successful keys for the MVP lifetime. JSON hashing normalizes field ordering; upload identity includes file checksum and metadata. Retry handling is transactional and safe under simultaneous duplicate requests. Resolve a completed retry before checking versions so an already successful request does not fail just because its original version is now stale.
- Mutable operations require the last-read version. Reject stale edits with `409`; return current data so the user or client can retry intentionally. Child mutations and status/archive actions also check and advance the application's version so a manual and automated update cannot silently overwrite one another. Child records expose their own versions where independently editable. Version checks and changes are atomic.
- Return understandable errors without exposing uploaded contents or stack traces. Record the input channel server-side and an optional client-supplied descriptive actor label. A reason's declared origin is separate metadata: labeling an entry `COMPANY_FEEDBACK` does not mean the application verified it with the employer.

Example later integration flow: create a `SAVED` application, upload a `PREPARED` CV, then explicitly record `APPLIED`, the applied date, and `SENT` usage once actual submission is known. After an interview, append a source note and then the structured Q&A. The application never treats a successful database write as evidence of employer submission.

## 7. Personal use and integration boundary

- No application authentication or authorization: no accounts, login screen, bearer tokens, roles, or per-user ownership checks. UI, API, statistics, and downloads work directly for the personal instance.
- Do not add Spring Security or an identity provider for this scope. Data integrity checks, validation, file format/size checks, version conflicts, and idempotency still apply; they are independent of user identity.
- Make the listen address and database connection configurable for the owner's deployment. PostgreSQL may run locally or as an external service; moving the application requires its configuration and database, not a local CV directory.
- A remote chat still needs a reachable application endpoint or a separately configured bridge. Network reachability and any scheduler are later integration work; merely having a chat open does not synchronize data.
- An external assistant or integration explicitly calls the API; its optional source/client label is descriptive. Do not imply authenticated identity or verified employer provenance.
- Keep database credentials outside source control. Do not log CV bytes, note/question contents, or salary data.
- The MVP does not send application data to any AI provider. External clients may provide AI assessments, clearly marked as such. Source labels describe the supplied origin; they do not prove the truth of employer feedback.

## 8. Technical approach

Recommended baseline:

| Component | Choice and purpose |
| --- | --- |
| Runtime/build | Java 21 LTS or newer, Maven Wrapper. Java 21 matches the installed runtime and the container image. |
| Application | Spring Boot 4.1.x, using a stable patch pinned when implementation begins. |
| UI | Spring MVC + Thymeleaf, ordinary HTML forms, one small local CSS stylesheet, and only small optional JavaScript enhancements. Core forms work without JavaScript. |
| Persistence | Spring JDBC, PostgreSQL, and Flyway migrations. Explicit SQL supports binary-file projections, full-text indexing, locking, and statistics. PostgreSQL is also used in integration tests to catch real database constraints. |
| Files | A separate PostgreSQL file table with `bytea` content, linked through `CvAttachment`; metadata projections avoid fetching bytes for list/detail queries. |
| Search | PostgreSQL search-document projection, stored generated `tsvector`, GIN index, and same-transaction refresh across related data. |
| Statistics | Typed status/events and reusable reason categories, with SQL-backed aggregate service/API reusable by the dashboard. |
| Validation | Shared field schemas and Spring MVC input validation, with field errors; no authentication/authorization dependency. |
| Delivery | Executable JAR and PostgreSQL via Docker Compose with a named database volume, or an externally configured PostgreSQL service. No application file volume is needed. |

Choose a single application with clear controller, service, and persistence responsibilities. Keep file persistence behind a small service so a later storage migration can preserve attachment IDs and download routes. There is no need for a separate JavaScript frontend, microservices, a queue, or an AI framework to deliver this scope.

The Spring Boot system requirements verify compatibility with the selected Java baseline. Spring's MVC guide demonstrates HTML rendering with Thymeleaf, and its upload guide demonstrates browser file handling. These support the technology choices; the product requirements above are proposed design decisions. Sources: [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html), [Spring MVC and Thymeleaf](https://spring.io/guides/gs/serving-web-content/), [Spring file uploads](https://spring.io/guides/gs/uploading-files/).

PostgreSQL documents [`bytea` for binary data](https://www.postgresql.org/docs/current/datatype-binary.html), [stored search vectors and GIN indexes](https://www.postgresql.org/docs/current/textsearch-tables.html), and [query parsing and relevance](https://www.postgresql.org/docs/current/textsearch-controls.html). Its [generated-column restrictions](https://www.postgresql.org/docs/current/ddl-generated-columns.html) explain why related-table content is first aggregated into a same-row search document.

## 9. Persistence, usability, and operational requirements

- All persistent records, including CV bytes, survive application restarts, rebuilds, container replacement, and deployment to a different device through the same or restored PostgreSQL database. Do not initialize an empty database over existing data.
- Flyway owns schema changes; do not rely on destructive automatic schema generation.
- File upload success is reported only after file bytes, metadata, the attachment link, the search projection, and any idempotency result commit in one database transaction. Rollback leaves no partial file or attachment. Transient request buffering is permitted; no persistent application-local directory is required or part of recovery.
- Document a PostgreSQL backup/restore using `pg_dump`/`pg_restore` or an equivalent service snapshot. One database backup contains business data, CV files, search data, and schema. Verify restoration into a separate instance using a sample application and downloaded-file checksum; no second filesystem backup or copied device-specific paths are needed.
- Keep configuration for database connection, listen address, timezone, and upload limit outside application code. No owner credentials or automation token are configured in the MVP.
- Use escaped plain-text output for descriptions, feedback, notes, Q&A, and reasons. Never render supplied HTML as trusted markup. External posting links allow only `http`/`https` schemes.
- Provide field-level errors, visible save confirmation, keyboard-accessible controls, labeled inputs, readable contrast, and multiline text areas. Failed validation preserves entered text.
- Keep tables usable on narrow screens through horizontal scrolling; detail forms stack vertically. No animation or decorative dashboard widgets.
- Use redirect-after-success for browser form submissions to avoid refresh resubmission. Show stale-edit conflicts with the attempted text available for comparison.
- Initial size target: 5,000 applications with associated rounds and notes. Dashboard queries are paginated and do not load attachment bytes or every application's interview history.

## 10. Acceptance criteria

The MVP is complete when all of the following can be demonstrated:

1. Create an application with only company and role. It appears in the dashboard as `SAVED`; unknown dates remain unknown.
2. Record multiple applications at one company, including the same role at a later date, without overwriting either record.
3. Edit role description, dates, feedback, notes, and expected/offered salary; retrieve the same values after restarting the app and database.
4. Filter, sort, and paginate the dashboard, including archived records when requested. Search an application using information found only in a question, answer, round note, role description, company feedback, rejection reason, historical status explanation, salary/date value, or CV filename; return one row per application with a matching-section excerpt.
5. Transition through statuses and then reopen a rejected application. Every previous status remains in history with source, effective time, and recorded time. The displayed result agrees with current status.
6. Enter an old application directly as rejected with unknown posting/applied dates; no invented dates or historical statuses appear.
7. Upload a prepared CV and mark it sent explicitly. Upload a second CV; both originals remain downloadable and match their recorded checksums after a restart. Verify that the bytes reside in PostgreSQL and list/detail queries do not fetch them.
8. Reject oversized files, unsupported formats, and malformed uploads without creating a file or attachment record. A database failure rolls back bytes, metadata, attachment link, search changes, and idempotency result together.
9. Add repeated technical rounds and a custom type; reuse the custom type in another application. Cancel a round without losing its notes or questions.
10. Add, edit, reorder, and remove erroneous Q&A entries, including unanswered questions and candidate-asked questions. Preserve multiline and non-ASCII text.
11. Save a pasted interview transcript immediately as a note; later attach structured Q&A with its source reference without replacing the original note.
12. Store employer feedback and multiple rejection hypotheses with reusable category IDs and rejection-event references. Missing likelihood remains missing, overlapping estimates need not total 100, and AI estimates are visibly labeled. Corrections preserve superseded reasons.
13. Perform the create/upload/status/round/note/Q&A/reason workflow through the API. Browser and API validations agree, and detail reads expose the stored information.
14. Retry successful API `POST` requests and send simultaneous duplicates with the same idempotency key; they create one logical record/upload/event. Reuse with different content returns `409`.
15. Submit a stale browser form or API update after a newer edit; receive a conflict and preserve the newer data. Wrong-application nested IDs are rejected.
16. Open both pages, download files, read statistics, and perform API operations without logging in, supplying an API token, or passing an authorization check. No login page or generated credential is present.
17. Archive and restore an application without changing its outcome, attachments, rounds, or history.
18. Back up PostgreSQL and restore it into a separate instance representing another device. Start a fresh application instance using only its configuration and restored database; verify business records, search results, statistics, and original CV checksums without copying an application file directory.
19. Complete the core dashboard and detail workflows with keyboard navigation and JavaScript disabled.
20. Change or remove a Q&A entry or edit a note through either input path; search immediately reflects committed changes. Rollback does not change search results. Concurrent changes to different child records do not lose either record's searchable content.
21. Find mixed-case text, non-English words, punctuation-bearing technical names, a URL fragment, an ISO date, and a salary amount. Null fields do not suppress other matches; empty search returns the ordinary dashboard; retained historical/superseded text stays searchable.
22. Rebuild search projections from relational records, including after a database restore/import, and obtain equivalent matches, filtering, pagination totals, and deterministic ordering.
23. With several rounds, questions, and reason entries per application, verify that application/status counts are not multiplied by joins. Reusable reason categories group differently worded descriptions correctly.
24. Verify distinct counts for currently rejected applications, applications ever rejected, and rejection events after rejection, reopening, and another rejection. Attribute each reason to its intended rejection event.
25. Verify that superseded and unassigned reasons are excluded from current-reason statistics, multiple assessments do not inflate category application counts, source groups remain separate, and rejections without a known reason remain countable. Search/archive/status filters affect both list and statistics consistently.

Validation should focus on PostgreSQL-backed service/API integration tests for history, transactional file storage, full-detail search refresh, accurate statistics, idempotency, and stale-write handling, plus a browser smoke test of the two templates and access without authentication. Avoid a large test suite that only duplicates straightforward field getters or template markup.

## 11. Implementation sequence after scope review

1. Establish the Spring Boot project, PostgreSQL/migrations, configuration, and core application/status model, without authentication or authorization.
2. Deliver the two page templates with application create/edit, the dashboard, status history, and archive support.
3. Add PostgreSQL-backed immutable CV uploads/downloads and money fields, then rounds, custom types, notes, and Q&A.
4. Add feedback, categorized rejection assessments, and same-transaction full-detail search projections/indexes for all related records.
5. Add the versioned API, idempotency, statistics aggregation service/summary endpoint, and documented integration examples.
6. Complete the acceptance checks, database transfer/recovery validation, and run documentation.

The confirmed PostgreSQL file storage, full-detail search, statistics foundation, and absence of authentication/authorization replace the earlier defaults. Other proposed choices, such as Java version and salary defaults, remain adjustable. No choice of future AI provider or dictation interface is necessary to implement the tracker described here.
