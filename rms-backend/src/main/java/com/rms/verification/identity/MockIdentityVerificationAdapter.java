package com.rms.verification.identity;

import com.rms.verification.VerificationResult;
import com.rms.verification.VerificationStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo identity provider. Mimics the real OTP-consent shape so the frontend
 * flow is identical when you swap in Fayda. Demo OTP is 123456.
 */
@Component("mockIdentity")
public class MockIdentityVerificationAdapter implements IdentityVerificationPort {

    private final ConcurrentHashMap<String, String> pending = new ConcurrentHashMap<>(); // txn -> faydaNumber

    @Override
    public IdentityChallenge initiate(String faydaNumber) {
        if (faydaNumber == null || !faydaNumber.trim().matches("^\\d{12}$")) {
            return new IdentityChallenge(null, VerificationStatus.FAILED, "Fayda number must be 12 digits");
        }
        String txn = UUID.randomUUID().toString();
        pending.put(txn, faydaNumber.trim());
        return new IdentityChallenge(txn, VerificationStatus.PENDING, "OTP sent (demo OTP: 123456)");
    }

    @Override
    public VerificationResult complete(String transactionId, String otp) {
        String fayda = pending.remove(transactionId);
        if (fayda == null) {
            return VerificationResult.failed(name(), "Unknown or expired transaction");
        }
        if (!"123456".equals(otp)) {
            return VerificationResult.failed(name(), "Incorrect OTP");
        }
        return VerificationResult.verified(name(), transactionId,
                Map.of("fullName", "Demo Resident", "faydaNumber", fayda));
    }

    @Override
    public String name() {
        return "mockIdentity";
    }
}
