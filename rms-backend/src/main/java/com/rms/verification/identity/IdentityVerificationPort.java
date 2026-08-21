package com.rms.verification.identity;

import com.rms.verification.VerificationResult;

/**
 * Two-step flow, shaped like Fayda eKYC:
 *   initiate() -> OTP/consent sent to the resident
 *   complete() -> submit OTP -> consented KYC attributes returned
 */
public interface IdentityVerificationPort {
    IdentityChallenge initiate(String faydaNumber);

    VerificationResult complete(String transactionId, String otp);

    String name();
}
