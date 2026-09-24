-- Migration: add version column to notifications table for optimistic locking
-- Date: 2026-09-13
-- Why: Hibernate version field requires database column for optimistic locking

-- Add version column with default value (MySQL syntax)
ALTER TABLE notifications ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
