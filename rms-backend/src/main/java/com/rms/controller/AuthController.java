package com.rms.controller;

import com.rms.dto.AuthRequest;
import com.rms.dto.AuthResponse;
import com.rms.dto.CreateUserRequest;
import com.rms.dto.UserResponse;
import com.rms.entity.User;
import com.rms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        System.out.println("🔐 CONTROLLER: Login attempt for user: " + request.getUsername());
        AuthResponse response = authService.login(request);
        System.out.println("✅ CONTROLLER: Login successful for: " + request.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request) {
        System.out.println("🔐 CONTROLLER: Register attempt for: " + request.getUsername());
        User user = authService.register(request);
        System.out.println("✅ CONTROLLER: Register successful for: " + request.getUsername());
        return UserResponse.from(user);
    }
}