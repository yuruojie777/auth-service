package com.roger.auth_service.repo;

import com.roger.auth_service.entity.AuthUser;
import com.roger.auth_service.entity.AuthUserProjectMembership;
import com.roger.auth_service.entity.AuthProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthUserProjectMembershipRepository extends JpaRepository<AuthUserProjectMembership, UUID> {

    Optional<AuthUserProjectMembership> findByUserAndProject(AuthUser user, AuthProject project);

    List<AuthUserProjectMembership> findByUser(AuthUser user);
}
