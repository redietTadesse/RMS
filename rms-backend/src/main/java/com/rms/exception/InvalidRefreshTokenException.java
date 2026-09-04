package com.rms.exception;

/**
 * Thrown when a refresh token is invalid, revoked, or expired.
 * Mapped to HTTP 401 by GlobalExceptionHandler so clients know the
 * session cannot be recovered and must re-authenticate.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
