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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = authService.register(request);
        return UserResponse.from(user);
    }

    @GetMapping("/me")
    public UserResponse getMyProfile(Principal principal) {
        User user = authService.getCurrentUser(principal.getName());
        return UserResponse.from(user);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(@Valid @RequestBody UpdatePasswordRequest request,
                                                Principal principal) {
        authService.changePassword(principal.getName(), request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.from(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @PatchMapping("/{id}/toggle-active")
    public UserResponse toggleActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setActive(!user.isActive());
        return UserResponse.from(userRepository.save(user));
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @RequestParam String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(Role.valueOf(role.toUpperCase()));
        return UserResponse.from(userRepository.save(user));
    }
}
