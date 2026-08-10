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

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public User register(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());  // ← ADD THIS

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

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return new AuthResponse(token, user.getUsername(), user.getName(), user.getRole().name());
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

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
}