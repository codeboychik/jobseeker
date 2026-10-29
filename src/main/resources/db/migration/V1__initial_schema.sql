CREATE TABLE applications (
 id uuid PRIMARY KEY, company text NOT NULL CHECK (length(trim(company)) > 0),
 role text NOT NULL CHECK (length(trim(role)) > 0), job_url text, external_reference text,
 posted_date date, applied_date date, description text, company_feedback text,
 status text NOT NULL CHECK (status IN ('SAVED','APPLIED','INTERVIEWING','OFFERED','ACCEPTED','REJECTED','WITHDRAWN','CLOSED')),
 expected_amount numeric(16,2), expected_currency text, expected_period text, expected_basis text,
 offered_amount numeric(16,2), offered_currency text, offered_period text, offered_basis text,
 archived boolean NOT NULL DEFAULT false, version bigint NOT NULL DEFAULT 0,
 source text NOT NULL, actor text, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 CHECK (expected_amount IS NULL OR (expected_amount > 0 AND expected_currency IS NOT NULL AND expected_period IS NOT NULL)),
 CHECK (offered_amount IS NULL OR (offered_amount > 0 AND offered_currency IS NOT NULL AND offered_period IS NOT NULL)),
 CHECK (expected_period IS NULL OR expected_period IN ('HOUR','MONTH','YEAR')),
 CHECK (offered_period IS NULL OR offered_period IN ('HOUR','MONTH','YEAR')),
 CHECK (expected_basis IS NULL OR expected_basis IN ('GROSS','NET','UNSPECIFIED')),
 CHECK (offered_basis IS NULL OR offered_basis IN ('GROSS','NET','UNSPECIFIED'))
);
CREATE INDEX applications_status_archive ON applications(status, archived);
CREATE INDEX applications_updated ON applications(updated_at DESC, id);
CREATE TABLE status_changes (
 id uuid PRIMARY KEY, application_id uuid NOT NULL REFERENCES applications(id),
 sequence bigint NOT NULL, previous_status text, new_status text NOT NULL,
 event_time timestamptz, recorded_at timestamptz NOT NULL DEFAULT now(), explanation text,
 source text NOT NULL, actor text, UNIQUE(application_id,sequence)
);
CREATE INDEX status_changes_type_time ON status_changes(new_status,event_time,application_id);
CREATE TABLE stored_files (
 id uuid PRIMARY KEY, content bytea NOT NULL CHECK (octet_length(content)>0),
 filename text NOT NULL, media_type text NOT NULL, size bigint NOT NULL,
 checksum char(64) NOT NULL, uploaded_at timestamptz NOT NULL DEFAULT now(),
 CHECK (size = octet_length(content))
);
CREATE TABLE cv_attachments (
 id uuid PRIMARY KEY, application_id uuid NOT NULL REFERENCES applications(id),
 file_id uuid NOT NULL UNIQUE REFERENCES stored_files(id),
 usage text NOT NULL CHECK (usage IN ('PREPARED','SENT')), sent_date date,
 source text NOT NULL, actor text, version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX cv_application ON cv_attachments(application_id);
CREATE TABLE round_types (
 id uuid PRIMARY KEY, name text NOT NULL, normalized_name text NOT NULL UNIQUE, predefined boolean NOT NULL DEFAULT false
);
CREATE TABLE rejection_reason_types (
 id uuid PRIMARY KEY, code text NOT NULL UNIQUE, name text NOT NULL, normalized_name text NOT NULL UNIQUE, predefined boolean NOT NULL DEFAULT false
);
CREATE TABLE interview_rounds (
 id uuid PRIMARY KEY, application_id uuid NOT NULL REFERENCES applications(id),
 type_id uuid NOT NULL REFERENCES round_types(id), title text, display_order integer NOT NULL DEFAULT 0,
 scheduled_at timestamptz, completed_at timestamptz,
 status text NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','COMPLETED','CANCELLED')),
 result text NOT NULL DEFAULT 'PENDING' CHECK (result IN ('PENDING','PASSED','FAILED','NOT_PROVIDED')),
 feedback text, version bigint NOT NULL DEFAULT 0, source text NOT NULL, actor text,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX rounds_application ON interview_rounds(application_id,display_order,id);
CREATE TABLE notes (
 id uuid PRIMARY KEY, application_id uuid NOT NULL REFERENCES applications(id),
 round_id uuid REFERENCES interview_rounds(id), text text NOT NULL CHECK (length(trim(text))>0),
 version bigint NOT NULL DEFAULT 0, source text NOT NULL, actor text,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX notes_application ON notes(application_id);
CREATE INDEX notes_round ON notes(round_id);
CREATE TABLE questions (
 id uuid PRIMARY KEY, round_id uuid NOT NULL REFERENCES interview_rounds(id),
 direction text NOT NULL CHECK (direction IN ('EMPLOYER_ASKED','CANDIDATE_ASKED')),
 question text NOT NULL CHECK (length(trim(question))>0), answer text, notes text,
 display_order integer NOT NULL DEFAULT 0, source_note_id uuid REFERENCES notes(id),
 version bigint NOT NULL DEFAULT 0, source text NOT NULL, actor text,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX questions_round ON questions(round_id,display_order,id);
CREATE INDEX questions_source_note ON questions(source_note_id);
CREATE TABLE rejection_reasons (
 id uuid PRIMARY KEY, application_id uuid NOT NULL REFERENCES applications(id),
 type_id uuid NOT NULL REFERENCES rejection_reason_types(id), rejection_event_id uuid REFERENCES status_changes(id),
 label text NOT NULL CHECK (length(trim(label))>0), explanation text,
 source text NOT NULL CHECK (source IN ('COMPANY_FEEDBACK','SELF_ASSESSMENT','AI_ASSESSMENT')),
 input_channel text NOT NULL, actor text, likelihood numeric(5,2) CHECK (likelihood BETWEEN 0 AND 100),
 evidence text, round_id uuid REFERENCES interview_rounds(id), question_id uuid REFERENCES questions(id) ON DELETE SET NULL,
 note_id uuid REFERENCES notes(id), assessed_at timestamptz, model text,
 supersedes_id uuid UNIQUE REFERENCES rejection_reasons(id), recorded_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX reasons_application ON rejection_reasons(application_id);
CREATE INDEX reasons_category_source_event ON rejection_reasons(type_id,source,rejection_event_id);
CREATE INDEX reasons_round ON rejection_reasons(round_id);
CREATE INDEX reasons_question ON rejection_reasons(question_id);
CREATE INDEX reasons_note ON rejection_reasons(note_id);
CREATE TABLE application_search (
 application_id uuid PRIMARY KEY REFERENCES applications(id), search_document text NOT NULL,
 sections jsonb NOT NULL, indexed_version bigint NOT NULL,
 search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple'::regconfig,search_document)) STORED
);
CREATE INDEX application_full_text ON application_search USING GIN(search_vector);
CREATE TABLE idempotency_records (
 scope text PRIMARY KEY, request_hash char(64) NOT NULL, response_status integer NOT NULL,
 response_body text NOT NULL, location text, completed_at timestamptz NOT NULL DEFAULT now()
);
CREATE FUNCTION reject_immutable_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Historical records and file bytes are immutable'; END $$;
CREATE TRIGGER immutable_status_history BEFORE UPDATE OR DELETE ON status_changes FOR EACH ROW EXECUTE FUNCTION reject_immutable_change();
CREATE TRIGGER immutable_files BEFORE UPDATE ON stored_files FOR EACH ROW EXECUTE FUNCTION reject_immutable_change();

INSERT INTO round_types(id,name,normalized_name,predefined) VALUES
 ('10000000-0000-0000-0000-000000000001','Pre-screen','pre-screen',true),
 ('10000000-0000-0000-0000-000000000002','Recruiter interview','recruiter interview',true),
 ('10000000-0000-0000-0000-000000000003','Technical interview','technical interview',true),
 ('10000000-0000-0000-0000-000000000004','Homework / take-home','homework / take-home',true),
 ('10000000-0000-0000-0000-000000000005','Behavioral interview','behavioral interview',true),
 ('10000000-0000-0000-0000-000000000006','Team fit','team fit',true),
 ('10000000-0000-0000-0000-000000000007','Cultural fit','cultural fit',true);
INSERT INTO rejection_reason_types(id,code,name,normalized_name,predefined) VALUES
 ('20000000-0000-0000-0000-000000000001','TECHNICAL_SKILLS','Technical skills','technical skills',true),
 ('20000000-0000-0000-0000-000000000002','EXPERIENCE','Experience','experience',true),
 ('20000000-0000-0000-0000-000000000003','COMPENSATION','Compensation mismatch','compensation mismatch',true),
 ('20000000-0000-0000-0000-000000000004','COMMUNICATION','Communication','communication',true),
 ('20000000-0000-0000-0000-000000000005','ROLE_FIT','Role / team fit','role / team fit',true),
 ('20000000-0000-0000-0000-000000000006','HIRING_CHANGE','Hiring change','hiring change',true),
 ('20000000-0000-0000-0000-000000000007','OTHER','Other','other',true);
