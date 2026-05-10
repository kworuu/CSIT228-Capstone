package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents an admin or staff user of the CivicGuard system.
 * Maps to the {@code users} table. The {@code passwordHash} field
 * stores a bcrypt hash, never plaintext.
 */
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private String displayName;
    private UserRole role;
    private Long assignedCenterId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public User() {}

    public User(Long id, String username, String passwordHash, String email,
                String displayName, UserRole role, Long assignedCenterId,
                LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.assignedCenterId = assignedCenterId;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Long getAssignedCenterId() { return assignedCenterId; }
    public void setAssignedCenterId(Long assignedCenterId) { this.assignedCenterId = assignedCenterId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    @Override
    public String toString() { return displayName + " (@" + username + ")"; }
}