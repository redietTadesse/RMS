package com.rms.verification;

import com.rms.verification.identity.IdentityChallenge;
import com.rms.verification.identity.IdentityVerificationPort;
import com.rms.verification.tin.TinVerificationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Single place that reads config and hands back the live adapter.
 * Callers never know which provider is active.
 */
@Service
public class VerificationService {

    private final Map<String, TinVerificationPort> tinProviders;          // keyed by bean name
    private final Map<String, IdentityVerificationPort> identityProviders; // keyed by bean name

    @Value("${verification.tin.provider:mockTin}")
    private String tinProvider;

    @Value("${verification.identity.provider:mockIdentity}")
    private String identityProvider;

    public VerificationService(Map<String, TinVerificationPort> tinProviders,
                               Map<String, IdentityVerificationPort> identityProviders) {
        this.tinProviders = tinProviders;
        this.identityProviders = identityProviders;
    }

    public VerificationResult verifyTin(String tin) {
        return tin().verify(tin);
    }

    public IdentityChallenge initiateIdentity(String faydaNumber) {
        return identity().initiate(faydaNumber);
    }

    public VerificationResult completeIdentity(String txn, String otp) {
        return identity().complete(txn, otp);
    }

    private TinVerificationPort tin() {
        TinVerificationPort p = tinProviders.get(tinProvider);
        if (p == null) {
            throw new IllegalStateException("No TIN provider bean named '" + tinProvider + "'");
        }
        return p;
    }

    private IdentityVerificationPort identity() {
        IdentityVerificationPort p = identityProviders.get(identityProvider);
        if (p == null) {
            throw new IllegalStateException("No identity provider bean named '" + identityProvider + "'");
        }
        return p;
    }
}
