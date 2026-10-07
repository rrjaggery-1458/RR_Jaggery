BEGIN;

CREATE TABLE IF NOT EXISTS finance_schema.form_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_key VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS finance_schema.form_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES finance_schema.form_definitions(id),
    field_key VARCHAR(100) NOT NULL,
    label VARCHAR(160) NOT NULL,
    field_type VARCHAR(30) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_managed BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    placeholder VARCHAR(160),
    help_text VARCHAR(1000),
    validation_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_finance_form_field_key UNIQUE (form_definition_id, field_key)
);
CREATE TABLE IF NOT EXISTS finance_schema.form_field_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_id UUID NOT NULL REFERENCES finance_schema.form_fields(id),
    option_value VARCHAR(120) NOT NULL,
    display_label VARCHAR(160) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_finance_form_option_value UNIQUE (field_id, option_value)
);
CREATE TABLE IF NOT EXISTS finance_schema.form_revisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES finance_schema.form_definitions(id),
    version INTEGER NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    CONSTRAINT uq_finance_form_revision UNIQUE (form_definition_id, version)
);
CREATE TABLE IF NOT EXISTS finance_schema.form_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_definition_id UUID NOT NULL REFERENCES finance_schema.form_definitions(id),
    form_revision_id UUID NOT NULL REFERENCES finance_schema.form_revisions(id),
    form_version INTEGER NOT NULL,
    record_type VARCHAR(80) NOT NULL,
    record_id UUID NOT NULL,
    answers_json TEXT NOT NULL,
    submitted_by VARCHAR(255) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS finance_schema.form_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES finance_schema.form_submissions(id),
    field_key VARCHAR(100) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(120) NOT NULL UNIQUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_finance_form_field_definition
    ON finance_schema.form_fields(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_finance_form_option_field
    ON finance_schema.form_field_options(field_id);
CREATE INDEX IF NOT EXISTS idx_finance_form_submission_record
    ON finance_schema.form_submissions(record_type, record_id);
CREATE INDEX IF NOT EXISTS idx_finance_form_submission_definition
    ON finance_schema.form_submissions(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_finance_form_attachment_submission
    ON finance_schema.form_attachments(submission_id);

CREATE TABLE IF NOT EXISTS finance_schema.investments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_name VARCHAR(200) NOT NULL,
    investment_type VARCHAR(120) NOT NULL,
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    investment_date DATE NOT NULL,
    details VARCHAR(1000),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_investment_date
    ON finance_schema.investments(investment_date);

CREATE TABLE IF NOT EXISTS customer_schema.form_definitions
    (LIKE finance_schema.form_definitions INCLUDING ALL);
CREATE TABLE IF NOT EXISTS customer_schema.form_fields
    (LIKE finance_schema.form_fields INCLUDING ALL);
CREATE TABLE IF NOT EXISTS customer_schema.form_field_options
    (LIKE finance_schema.form_field_options INCLUDING ALL);
CREATE TABLE IF NOT EXISTS customer_schema.form_revisions
    (LIKE finance_schema.form_revisions INCLUDING ALL);
CREATE TABLE IF NOT EXISTS customer_schema.form_submissions
    (LIKE finance_schema.form_submissions INCLUDING ALL);
CREATE TABLE IF NOT EXISTS customer_schema.form_attachments
    (LIKE finance_schema.form_attachments INCLUDING ALL);

CREATE TABLE IF NOT EXISTS production_schema.form_definitions
    (LIKE finance_schema.form_definitions INCLUDING ALL);
CREATE TABLE IF NOT EXISTS production_schema.form_fields
    (LIKE finance_schema.form_fields INCLUDING ALL);
CREATE TABLE IF NOT EXISTS production_schema.form_field_options
    (LIKE finance_schema.form_field_options INCLUDING ALL);
CREATE TABLE IF NOT EXISTS production_schema.form_revisions
    (LIKE finance_schema.form_revisions INCLUDING ALL);
CREATE TABLE IF NOT EXISTS production_schema.form_submissions
    (LIKE finance_schema.form_submissions INCLUDING ALL);
CREATE TABLE IF NOT EXISTS production_schema.form_attachments
    (LIKE finance_schema.form_attachments INCLUDING ALL);

ALTER TABLE customer_schema.form_fields
    ADD CONSTRAINT fk_customer_form_field_definition
    FOREIGN KEY (form_definition_id) REFERENCES customer_schema.form_definitions(id);
ALTER TABLE customer_schema.form_field_options
    ADD CONSTRAINT fk_customer_form_option_field
    FOREIGN KEY (field_id) REFERENCES customer_schema.form_fields(id);
ALTER TABLE customer_schema.form_revisions
    ADD CONSTRAINT fk_customer_form_revision_definition
    FOREIGN KEY (form_definition_id) REFERENCES customer_schema.form_definitions(id);
ALTER TABLE customer_schema.form_submissions
    ADD CONSTRAINT fk_customer_form_submission_definition
    FOREIGN KEY (form_definition_id) REFERENCES customer_schema.form_definitions(id);
ALTER TABLE customer_schema.form_submissions
    ADD CONSTRAINT fk_customer_form_submission_revision
    FOREIGN KEY (form_revision_id) REFERENCES customer_schema.form_revisions(id);
ALTER TABLE customer_schema.form_attachments
    ADD CONSTRAINT fk_customer_form_attachment_submission
    FOREIGN KEY (submission_id) REFERENCES customer_schema.form_submissions(id);

ALTER TABLE production_schema.form_fields
    ADD CONSTRAINT fk_production_form_field_definition
    FOREIGN KEY (form_definition_id) REFERENCES production_schema.form_definitions(id);
ALTER TABLE production_schema.form_field_options
    ADD CONSTRAINT fk_production_form_option_field
    FOREIGN KEY (field_id) REFERENCES production_schema.form_fields(id);
ALTER TABLE production_schema.form_revisions
    ADD CONSTRAINT fk_production_form_revision_definition
    FOREIGN KEY (form_definition_id) REFERENCES production_schema.form_definitions(id);
ALTER TABLE production_schema.form_submissions
    ADD CONSTRAINT fk_production_form_submission_definition
    FOREIGN KEY (form_definition_id) REFERENCES production_schema.form_definitions(id);
ALTER TABLE production_schema.form_submissions
    ADD CONSTRAINT fk_production_form_submission_revision
    FOREIGN KEY (form_revision_id) REFERENCES production_schema.form_revisions(id);
ALTER TABLE production_schema.form_attachments
    ADD CONSTRAINT fk_production_form_attachment_submission
    FOREIGN KEY (submission_id) REFERENCES production_schema.form_submissions(id);

CREATE INDEX IF NOT EXISTS idx_customer_form_field_definition
    ON customer_schema.form_fields(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_customer_form_option_field
    ON customer_schema.form_field_options(field_id);
CREATE INDEX IF NOT EXISTS idx_customer_form_submission_record
    ON customer_schema.form_submissions(record_type, record_id);
CREATE INDEX IF NOT EXISTS idx_customer_form_submission_definition
    ON customer_schema.form_submissions(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_customer_form_attachment_submission
    ON customer_schema.form_attachments(submission_id);
CREATE INDEX IF NOT EXISTS idx_production_form_field_definition
    ON production_schema.form_fields(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_production_form_option_field
    ON production_schema.form_field_options(field_id);
CREATE INDEX IF NOT EXISTS idx_production_form_submission_record
    ON production_schema.form_submissions(record_type, record_id);
CREATE INDEX IF NOT EXISTS idx_production_form_submission_definition
    ON production_schema.form_submissions(form_definition_id);
CREATE INDEX IF NOT EXISTS idx_production_form_attachment_submission
    ON production_schema.form_attachments(submission_id);

COMMIT;
