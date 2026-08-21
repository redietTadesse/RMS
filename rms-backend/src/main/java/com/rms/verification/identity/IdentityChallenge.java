package com.rms.verification.identity;

import com.rms.verification.VerificationStatus;

public record IdentityChallenge(
        String transactionId,
        VerificationStatus status,
        String message
) {}
