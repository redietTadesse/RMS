package com.rms.verification.tin;

import com.rms.verification.VerificationResult;
import org.springframework.stereotype.Component;

/**
 * Go-live TIN provider. Wire a paid provider (Lookuptax / TaxDo / tin-check)
 * or an official Ministry of Revenue endpoint here. Callers never change.
 */
@Component("externalTin")
public class ExternalTinVerificationAdapter implements TinVerificationPort {

    // Inject base-url + api-key from config when you go live, e.g. via
    // @Value or a @ConfigurationProperties bean, plus a RestClient/RestTemplate.
    // private final RestClient client;   // Spring Boot 3.2+

    @Override
    public VerificationResult verify(String tin) {
        // TODO (go-live):
        //   var resp = client.post().uri("/verify").body(Map.of("tin", tin)).retrieve().body(...);
        //   map their response -> VerificationResult.verified(...) / .failed(...)
        return VerificationResult.error(name(), "External TIN provider not configured yet");
    }

    @Override
    public String name() {
        return "externalTin";
    }
}
