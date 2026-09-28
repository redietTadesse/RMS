-- Migration: add SUBMITTED to invoices.status enum
-- Date: 2026-09-25
-- Why: InvoiceStatus gained SUBMITTED (receipt submitted, awaiting admin approval)
--      but ddl-auto=update does NOT modify existing column types, so the MySQL
--      column stayed at enum('OVERDUE','PAID','PENDING'). Any invoice update that
--      writes SUBMITTED throws:
--        "Data truncated for column 'status' at row 1" (SQL error 1265)
--      Triggered by PaymentService.recordPayment() when a tenant uploads a receipt.

ALTER TABLE invoices
    MODIFY status ENUM('PENDING','SUBMITTED','PAID','OVERDUE') NOT NULL DEFAULT 'PENDING';
