BEGIN;

ALTER TABLE auth_schema.users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS auth_schema.registration_email_otps (
    id UUID PRIMARY KEY,
    challenge_token UUID NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    business_name VARCHAR(150),
    gstin VARCHAR(20),
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    last_sent_at TIMESTAMPTZ NOT NULL,
    send_window_started_at TIMESTAMPTZ NOT NULL,
    send_count INTEGER NOT NULL,
    attempts INTEGER NOT NULL
);

COMMIT;
