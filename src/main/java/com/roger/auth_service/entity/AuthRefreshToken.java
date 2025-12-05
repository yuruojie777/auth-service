package com.roger.auth_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_refresh_token",
        indexes = {
                @Index(name = "idx_refresh_user", columnList = "userId"),
                @Index(name = "idx_refresh_expires", columnList = "expiresAt")
        })
@Getter
@Setter
public class AuthRefreshToken {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String projectId;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column
    private Instant revokedAt;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    private String userAgent;
    private String ipAddress;
}
