package com.sachin.aiassistant.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"))
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(nullable = false, length = 180) private String email;
    @Column(nullable = false) private String password;
    @Column(nullable = false, length = 30) private String role;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false) private Organization organization;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected AppUser() {}
    public AppUser(String fullName, String email, String password, String role, Organization organization) {
        this.fullName = fullName; this.email = email; this.password = password; this.role = role; this.organization = organization;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public UUID getId() { return id; } public String getFullName() { return fullName; } public String getEmail() { return email; }
    public String getPassword() { return password; } public String getRole() { return role; } public Organization getOrganization() { return organization; }
    public Instant getCreatedAt() { return createdAt; }
}
