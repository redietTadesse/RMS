package com.rms.verification.tin;

import com.rms.verification.VerificationResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Demo TIN provider. No network call. Bean name "mockTin" is the config key.
 */
@Component("mockTin")
public class MockTinVerificationAdapter implements TinVerificationPort {

    private static final Pattern FORMAT = Pattern.compile("^\\d{10}$");

    @Override
    public VerificationResult verify(String tin) {
        if (tin == null || !FORMAT.matcher(tin.trim()).matches()) {
            return VerificationResult.failed(name(), "TIN must be exactly 10 digits");
        }
        // Deterministic demo rule: TINs starting 0000 simulate "not registered".
        if (tin.trim().startsWith("0000")) {
            return VerificationResult.failed(name(), "Not registered (demo)");
        }
        return VerificationResult.verified(name(), "MOCK-" + tin.trim(),
                Map.of("taxpayerName", "Demo Taxpayer", "region", "Addis Ababa"));
    }

    @Override
    public String name() {
        return "mockTin";
    }
}
