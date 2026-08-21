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
    private final RefreshTokenService refreshTokenService;  // ✅ ADD THIS

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       RefreshTokenService refreshTokenService) {  // ✅ ADD
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    public User register(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        Role role = Role.TENANT;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid role: " + request.getRole());
            }
        }
        user.setRole(role);
        user.setActive(request.getActive() == null ? true : request.getActive());

        return userRepository.save(user);
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

        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        return response;
    }

    public AuthResponse refreshToken(String refreshToken) {
        User user = refreshTokenService.validateAndGetUser(refreshToken);
        String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        // Optionally rotate refresh token:
        // String newRefreshToken = refreshTokenService.createRefreshToken(user);
        // Then revoke the old one automatically via deleteByUser in createRefreshToken.

        AuthResponse response = new AuthResponse();
        response.setToken(newAccessToken);
        // If you rotate refresh token, set it here:
        // response.setRefreshToken(newRefreshToken);
        // Otherwise, keep the same one or set to null:
        // response.setRefreshToken(refreshToken); // or null if not rotating
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        return response;
    }

    @Transactional
    public void logout(String username) {
        User user = getCurrentUser(username);
        refreshTokenService.revokeAllUserTokens(user);
    }

    public User getCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
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
            throw new IllegalArgumentException("New password confirmation does not match");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}