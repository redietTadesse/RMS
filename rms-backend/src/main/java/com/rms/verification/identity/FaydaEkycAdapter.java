package com.rms.verification.identity;

import com.rms.verification.VerificationResult;
import com.rms.verification.VerificationStatus;
import org.springframework.stereotype.Component;

/**
 * Go-live identity provider. Wire Fayda eKYC (via your FISP) + eSignet/OIDC here.
 * partner-api-key + FISP endpoint injected from config. Callers never change.
 */
@Component("faydaIdentity")
public class FaydaEkycAdapter implements IdentityVerificationPort {

    @Override
    public IdentityChallenge initiate(String faydaNumber) {
        // TODO (go-live): POST eKYC auth request via FISP -> Fayda sends OTP -> return their transaction id.
        return new IdentityChallenge(null, VerificationStatus.ERROR, "Fayda not configured yet");
    }

    @Override
    public VerificationResult complete(String transactionId, String otp) {
        // TODO (go-live): submit OTP -> receive consented KYC attributes -> map to VerificationResult.verified(...)
        return VerificationResult.error(name(), "Fayda not configured yet");
    }

    @Override
    public String name() {
        return "faydaIdentity";
    }
}
