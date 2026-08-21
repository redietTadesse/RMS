package com.rms.verification.tin;

import com.rms.verification.VerificationResult;

public interface TinVerificationPort {
    VerificationResult verify(String tin);

    String name();
}
