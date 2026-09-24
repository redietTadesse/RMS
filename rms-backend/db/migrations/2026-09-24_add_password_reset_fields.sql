-- Migration: add password reset fields to users
-- Date: 2026-09-24
-- The reset service stores a short-lived, single-use token on the user.

ALTER TABLE users
    ADD COLUMN reset_token VARCHAR(36) NULL,
    ADD COLUMN reset_token_expiry DATETIME NULL;

CREATE UNIQUE INDEX idx_users_reset_token ON users (reset_token);
