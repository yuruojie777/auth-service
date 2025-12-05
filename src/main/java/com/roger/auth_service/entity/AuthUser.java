package com.roger.auth_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "auth_user",
        indexes = {
                @Index(name = "idx_auth_user_email", columnList = "email"),
                @Index(name = "idx_auth_user_active", columnList = "active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_user_email", columnNames = "email")
        }
)
@Getter
@Setter
public class AuthUser implements UserDetails {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @JsonIgnore // important: don't serialize the hash in /me
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean locked = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    @Column(columnDefinition = "timestamptz")
    private Instant lastLoginAt;

    @Column(nullable = false)
    private boolean deleted = false;

    /* ====== UserDetails mapping ====== */

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // project roles are in membership/JWT, so we can return empty here
        // and add authorities later from JWT in the filter if needed
        return List.of();
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        // we treat email as username
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true; // no separate expiry field yet
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        // disabled if inactive or soft-deleted
        return active && !deleted;
    }
}
