package com.rms.entity;

public enum InvoiceStatus {
    PENDING,
    SUBMITTED,
    PAID,
    OVERDUE;

    /**
     * Canonical invoice status state machine.
     * Single source of truth used by PaymentService and ArifPayService.
     *
     * PENDING   -> SUBMITTED (tenant submits receipt) | OVERDUE (past due) | PAID (direct gateway payment)
     * SUBMITTED -> PAID (admin approval)
     * OVERDUE   -> SUBMITTED (late receipt) | PAID (late gateway payment)
     * PAID      -> terminal, no transitions
     */
    public boolean canTransitionTo(InvoiceStatus to) {
        switch (this) {
            case PENDING:
                return to == SUBMITTED || to == OVERDUE || to == PAID;
            case SUBMITTED:
                return to == PAID;
            case OVERDUE:
                return to == SUBMITTED || to == PAID;
            case PAID:
                return false;
            default:
                return false;
        }
    }
}
