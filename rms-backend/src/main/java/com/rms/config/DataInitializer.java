package com.rms.config;

import com.rms.entity.Role;
import com.rms.entity.User;
import com.rms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataInitializer creates a default admin user if one doesn't exist.
 * This solves the chicken-and-egg problem of needing an admin to create the first admin.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check if admin user exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setName("System Administrator");
            admin.setEmail("admin@rms.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("✅ Default admin user created: username=admin, password=admin123");
        } else {
            System.out.println("ℹ️ Admin user already exists");
        }
    }
}
