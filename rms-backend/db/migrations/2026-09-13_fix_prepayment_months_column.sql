-- Migration: fix prepayment_months column default value
-- Date: 2026-09-13
-- Why: The prepayment_months column doesn't have a default value, causing insert errors

-- Set default value for the column
ALTER TABLE leases ALTER COLUMN prepayment_months SET DEFAULT 3;

-- Ensure all existing rows have a value
UPDATE leases SET prepayment_months = 3 WHERE prepayment_months IS NULL;

-- Set NOT NULL constraint
ALTER TABLE leases ALTER COLUMN prepayment_months SET NOT NULL;
