package com.rms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Name can only contain letters, spaces, dots, apostrophes, and hyphens")
    private String name;

    @Email(message = "Invalid email address format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    private Boolean active;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}