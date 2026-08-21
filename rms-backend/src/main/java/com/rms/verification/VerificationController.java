package com.rms.verification;

import com.rms.verification.identity.IdentityChallenge;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService service;

    public VerificationController(VerificationService service) {
        this.service = service;
    }

    @PostMapping("/tin")
    public VerificationResult verifyTin(@RequestBody Map<String, String> body) {
        return service.verifyTin(body.get("tin"));
    }

    @PostMapping("/identity/initiate")
    public IdentityChallenge initiate(@RequestBody Map<String, String> body) {
        return service.initiateIdentity(body.get("faydaNumber"));
    }

    @PostMapping("/identity/complete")
    public VerificationResult complete(@RequestBody Map<String, String> body) {
        return service.completeIdentity(body.get("transactionId"), body.get("otp"));
    }
}
