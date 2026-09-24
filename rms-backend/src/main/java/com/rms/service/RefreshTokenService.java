package com.rms.service;

import com.rms.entity.RefreshToken;
import com.rms.entity.User;
import com.rms.exception.InvalidRefreshTokenException;
import com.rms.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public String createRefreshToken(User user) {
        return createRefreshToken(user, null);
    }

    @Transactional
    public String createRefreshToken(User user, String presentedRefreshToken) {
        if (presentedRefreshToken != null && !presentedRefreshToken.isBlank()) {
            refreshTokenRepository.deleteByToken(hash(presentedRefreshToken));
        }

        // Store only a SHA-256 hash of the refresh token: a database dump must
        // never yield usable session credentials. The raw token lives only with
        // the client.
        String rawToken = UUID.randomUUID().toString();
        RefreshToken token = new RefreshToken();
        token.setToken(hash(rawToken));
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plus(Duration.ofMillis(refreshExpiration)));
        token.setRevoked(false);

        refreshTokenRepository.save(token);
        return rawToken;
    }

    public User validateAndGetUser(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(hash(refreshToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired session. Please sign in again."));

        if (token.isRevoked()) {
            throw new InvalidRefreshTokenException("This session has been signed out. Please sign in again.");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Your session has expired. Please sign in again.");
        }

        return token.getUser();
    }

    /**
     * Revokes a single refresh token (by hashing the raw value). Used by the
     * public logout endpoint, which must work even without a valid access token.
     */
    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.deleteByToken(hash(rawRefreshToken));
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    // Housekeeping: purge expired refresh tokens daily so the table cannot
    // grow unbounded for users who never log in again.
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        try {
            long removed = refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
            if (removed > 0) {
                log.info("Purged {} expired refresh token(s)", removed);
            }
        } catch (Exception e) {
            log.warn("Failed to purge expired refresh tokens", e);
        }
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is part of every standard JVM; unreachable in practice.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}