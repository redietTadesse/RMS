-- Migration: fix NULL boolean values on tenants / leases
-- Date: 2026-09-05
-- Why: columns were added via ALTER TABLE without NOT NULL, so legacy rows have
--      NULLs. The corresponding entity fields were primitive booleans, which
--      made Hibernate throw "Null value was assigned to a property of primitive
--      type" on load. Entity fields are now Boolean wrappers (null-safe), but
--      these NULLs should still be normalized and constrained.

-- 1) Backfill NULLs with FALSE
UPDATE tenants SET
    has_id_card                     = COALESCE(has_id_card, FALSE),
    has_business_license            = COALESCE(has_business_license, FALSE),
    is_withholding_tax_applicable   = COALESCE(is_withholding_tax_applicable, FALSE),
    withholding_evidence_verified   = COALESCE(withholding_evidence_verified, FALSE);

UPDATE leases SET
    payment_schedule_active         = COALESCE(payment_schedule_active, FALSE),
    first_invoice_prorated          = COALESCE(first_invoice_prorated, FALSE);

-- 2) Enforce NOT NULL going forward
ALTER TABLE tenants ALTER COLUMN has_id_card SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN has_business_license SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN is_withholding_tax_applicable SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN withholding_evidence_verified SET NOT NULL;

ALTER TABLE leases ALTER COLUMN payment_schedule_active SET NOT NULL;
ALTER TABLE leases ALTER COLUMN first_invoice_prorated SET NOT NULL;
