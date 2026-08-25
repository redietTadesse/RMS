package com.rms.controller;

import com.rms.dto.CreateUserRequest;
import com.rms.dto.UpdatePasswordRequest;
import com.rms.dto.UpdateUserRequest;
import com.rms.dto.UserResponse;
import com.rms.entity.Role;
import com.rms.entity.User;
import com.rms.repository.UserRepository;
import com.rms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    // ─── ADMIN ENDPOINTS ───

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = authService.register(request);
        return UserResponse.from(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.from(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse toggleActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(!user.isActive());
        return UserResponse.from(userRepository.save(user));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@PathVariable Long id, @RequestParam String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(Role.valueOf(role.toUpperCase()));
        return UserResponse.from(userRepository.save(user));
    }

    // ─── SELF-SERVICE ENDPOINTS ───

    /**
     * GET /api/users/me - Get current user profile
     * This returns the full user data for session management
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 Fetching profile for user: " + username);

            User user = authService.getCurrentUser(username);
            if (user == null) {
                System.out.println("❌ User not found: " + username);
                return ResponseEntity.notFound().build();
            }

            System.out.println("✅ Profile fetched for: " + username);
            return ResponseEntity.ok(UserResponse.from(user));
        } catch (Exception e) {
            System.err.println("❌ Error fetching profile: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/users/me - Update current user profile
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 Updating profile for user: " + username);

            User user = authService.getCurrentUser(username);

            if (request.getName() != null && !request.getName().isBlank()) {
                user.setName(request.getName());
            }
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                user.setEmail(request.getEmail());
            }

            User updated = userRepository.save(user);
            System.out.println("✅ Profile updated for: " + username);
            return ResponseEntity.ok(UserResponse.from(updated));
        } catch (Exception e) {
            System.err.println("❌ Error updating profile: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/users/me/password - Change password
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            Authentication authentication,
            @Valid @RequestBody UpdatePasswordRequest request) {
        try {
            String username = authentication.getName();
            System.out.println("🔍 Changing password for user: " + username);

            authService.changePassword(
                    username,
                    request.getCurrentPassword(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );
            System.out.println("✅ Password changed for: " + username);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.err.println("❌ Error changing password: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}