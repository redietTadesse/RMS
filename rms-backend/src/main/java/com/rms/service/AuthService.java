package com.rms.service;

import com.rms.dto.AuthRequest;
import com.rms.dto.AuthResponse;
import com.rms.dto.CreateUserRequest;
import com.rms.entity.Role;
import com.rms.entity.User;
import com.rms.repository.UserRepository;
import com.rms.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        User user = refreshTokenService.validateAndGetUser(refreshToken);
        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @Transactional
    public User register(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        Role role = Role.TENANT;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }
        user.setRole(role);
        user.setActive(request.getActive() == null || request.getActive());

        // Send welcome email with credentials before hashing password
        String plainPassword = request.getPassword();
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendWelcomeEmail(user, request.getUsername(), plainPassword);
        }

        user.setPassword(passwordEncoder.encode(plainPassword));
        return userRepository.save(user);
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        User user = getCurrentUser(username);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void requestPasswordReset(String usernameOrEmail) {
        var userOpt = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));

        if (userOpt.isEmpty()) {
            return; // silent no-op — don't reveal account existence
        }

        User user = userOpt.get();

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("This account has no email on file. Contact an administrator.");
        }

        String token = java.util.UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired reset link");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
    @Transactional
    public void logout(String username) {
        User user = getCurrentUser(username);
        refreshTokenService.revokeAllUserTokens(user);
    }

    /**
     * Logout for callers whose access token may already be expired: revoke
     * just the presented refresh token instead of every session of the user.
     */
    @Transactional
    public void logoutToken(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }
}