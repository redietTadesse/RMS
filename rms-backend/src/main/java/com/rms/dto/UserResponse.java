package com.rms.dto;

import com.rms.entity.User;
import com.rms.entity.Role;

public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String role;
    private boolean active;

    public UserResponse() {
    }

    public UserResponse(Long id, String username, String name, String role, boolean active) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.role = role;
        this.active = active;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole().name(),
                user.isActive()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
