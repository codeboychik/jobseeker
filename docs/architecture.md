# Code structure

The application is organized by feature. Each feature keeps its services, repositories, input schemas, and small domain types together. HTTP controllers and Thymeleaf support are separate adapters. The root package contains only the Spring Boot entry point.

```text
src/main/java/dev/jobseeker/
├── JobseekerApplication.java
├── application/       Application lifecycle, status history, salaries, detail assembly
├── catalog/           Reusable interview types and rejection categories
├── cv/                CV attachments, file persistence, upload validation
│   └── validation/    PDF and DOCX content validation
├── idempotency/       Transactional request replay and persisted responses
├── infrastructure/
│   ├── jdbc/          JDBC execution and row/column mapping
│   └── json/          JSON encoding and canonical request hashing
├── interview/         Interview rounds and question/answer operations
├── note/              Application and round notes
├── rejection/         Categorized assessments, evidence, and supersession
├── search/            Filters, search queries, index documents, result excerpts
├── shared/
│   ├── audit/         Input channels and audit metadata
│   ├── crypto/        File/request checksums
│   ├── error/         Application errors independent of HTTP rendering
│   ├── text/          Field names and readable labels
│   └── validation/    Reusable field rules, patch parsing, version checks
├── statistics/        Aggregate SQL and metric definitions
└── web/
    ├── api/           Resource-specific JSON controllers and response handling
    ├── error/         HTTP exception translation
    └── mvc/           Page and explicit form-action controllers
        └── support/   View assembly, select options, form parsing/error recovery

src/main/resources/
├── db/migration/      Flyway schema and reference data
├── templates/applications/
│   ├── dashboard.html
│   ├── detail.html
│   └── fragments/forms.html
└── static/            Stylesheet and OpenAPI contract

src/test/java/dev/jobseeker/integration/
└── JobseekerIntegrationTest.java
```

## Class responsibilities

- Controllers translate HTTP input and delegate to a feature service. API endpoints and browser forms share the same business services. Every form action has an explicit mapping; there is no catch-all action switch.
- Services coordinate validation, ownership checks, and transactions. `ApplicationService` handles application fields, status, and archive state; interview, question, note, CV, and rejection operations have their own services.
- Repositories own SQL and database access. The shared JDBC infrastructure only executes statements and maps rows. Controllers and services do not embed SQL.
- `ApplicationDetailService` composes read projections from the feature repositories. `ApplicationDetailViewAssembler` adds browser-only lookup options and presentation state.
- Field schemas live beside the feature that owns them. Generic validation supports omitted versus explicit-null PATCH fields. Enums define the allowed domain values.
- Validated patch maps and JDBC read projections retain the existing JSON/template field contract. They are not exposed as generic CRUD endpoints: each operation has an explicit service method and schema.
- `CvFileValidator` checks filename/size and delegates format-specific parsing to PDF and DOCX validators. `StoredFileRepository` alone persists/retrieves the immutable binary data.
- Statistics queries remain separate from the search projection. Search filtering is shared through `ApplicationSearchSql`, so list and statistics filters stay consistent.

## Transaction boundaries

Mutating feature-service methods are transactional. `ApplicationMutationCoordinator` requires an existing transaction, locks and checks the parent application version before child writes, and advances the parent version after a change. `SearchIndexService.refresh` also requires that transaction. Data changes and refreshed search documents therefore commit or roll back together.

`IdempotencyService` wraps API POST execution in the same transaction as the replay record. Standalone detail, search, and statistics reads use repeatable-read snapshots; they join an existing write transaction when composing a mutation response.

This refactor does not change the database schema, external URLs, JSON field names, upload storage, or the two-page workflow.

## Formatting and verification

```sh
./mvnw spotless:apply
./mvnw verify
docker compose up --build -d
```

Use `mvnw.cmd` on Windows. Spotless pins Google Java Format 1.28.0 with AOSP four-space indentation and checks formatting during `verify`. `.editorconfig` supplies consistent whitespace settings. Run verification against the disposable PostgreSQL test setup described in the README. Logs belong in `.logs/`; they are excluded from Git and Docker build contexts.
