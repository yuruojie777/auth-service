package com.roger.auth_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "auth_user_project_membership",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_project",
                        columnNames = {"user_id", "project_id"}
                )
        }
)
@Getter
@Setter
public class AuthUserProjectMembership {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AuthUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id")
    private AuthProject project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProjectRole role = ProjectRole.USER;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
