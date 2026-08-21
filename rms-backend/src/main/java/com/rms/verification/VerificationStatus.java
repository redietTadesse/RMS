package com.rms.verification;

public enum VerificationStatus {
    UNVERIFIED,   // never checked
    PENDING,      // awaiting OTP/consent (Fayda flow)
    VERIFIED,     // confirmed
    FAILED,       // checked, not valid / not registered
    ERROR         // provider unreachable / misconfigured
}
