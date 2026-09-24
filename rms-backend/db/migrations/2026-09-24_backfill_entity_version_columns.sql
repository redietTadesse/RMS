-- Migration: backfill version columns added by Hibernate ddl-auto=update
-- Date: 2026-09-24
-- Why: ddl-auto=update added nullable `version` columns WITHOUT backfilling
--      existing rows. Any save/update on a row with version = NULL throws:
--      "Cannot invoke java.lang.Long.longValue() because current is null"
--      (Hibernate optimistic-lock version unboxing NPE). This broke
--      generateMonthlyInvoices / any update on leases, invoices, notifications, spaces.

-- 1. Backfill existing rows with version 0 (matches entity default `= 0L`)
UPDATE invoices      SET version = 0 WHERE version IS NULL;
UPDATE leases        SET version = 0 WHERE version IS NULL;
UPDATE notifications SET version = 0 WHERE version IS NULL;
UPDATE spaces        SET version = 0 WHERE version IS NULL;

-- 2. Make the columns NOT NULL with a default so new rows never get NULL
ALTER TABLE invoices      MODIFY version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE leases        MODIFY version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE notifications MODIFY version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE spaces        MODIFY version BIGINT NOT NULL DEFAULT 0;

-- 3. Backfill legacy lease billing fields added after these leases were created.
--    Without them, saving a legacy lease fails bean validation
--    ("Billing frequency is required") or NPEs unboxing initial_payment_months
--    inside updateNextBillingDate when generating invoices.
UPDATE leases SET billing_frequency = 'MONTHLY' WHERE billing_frequency IS NULL;
UPDATE leases SET initial_payment_months = 1 WHERE initial_payment_months IS NULL;

