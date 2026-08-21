package com.rms.verification;

import java.util.Map;

public record VerificationResult(
        VerificationStatus status,
        String provider,
        String referenceId,              // provider txn/lookup id, for audit
        Map<String, String> attributes,  // neutral: name, region, etc.
        String message
) {
    public static VerificationResult verified(String provider, String ref, Map<String, String> attrs) {
        return new VerificationResult(VerificationStatus.VERIFIED, provider, ref, attrs, "Verified");
    }

    public static VerificationResult failed(String provider, String msg) {
        return new VerificationResult(VerificationStatus.FAILED, provider, null, Map.of(), msg);
    }

    public static VerificationResult error(String provider, String msg) {
        return new VerificationResult(VerificationStatus.ERROR, provider, null, Map.of(), msg);
    }
}
