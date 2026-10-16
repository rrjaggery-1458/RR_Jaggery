ALTER TABLE auth_schema.registration_email_otps
    ADD COLUMN IF NOT EXISTS email_delivery_status VARCHAR(20) NOT NULL DEFAULT 'SENT';
