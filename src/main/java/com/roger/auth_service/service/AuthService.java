package com.roger.auth_service.service;

import com.roger.auth_service.dto.AuthTokensDTO;
import com.roger.auth_service.entity.*;
import com.roger.auth_service.exception.EmailAlreadyUsedException;
import com.roger.auth_service.exception.InvalidCredentialsException;
import com.roger.auth_service.exception.ProjectAccessDeniedException;
import com.roger.auth_service.exception.ProjectNotFoundException;
import com.roger.auth_service.repo.AuthProjectRepository;
import com.roger.auth_service.repo.AuthUserProjectMembershipRepository;
import com.roger.auth_service.repo.AuthUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthUserRepository userRepository;
    private final AuthProjectRepository projectRepository;
    private final AuthUserProjectMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // 注册：用户 + 项目 membership + JWT（project_id + roles）+ refresh token
    public AuthTokensDTO register(String email,
                                  String rawPassword,
                                  String projectId,
                                  String userAgent,
                                  String ip) {

        log.debug("Register user email={} projectId={}", email, projectId);

        AuthProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        AuthUser user = new AuthUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        AuthUser saved = userRepository.save(user);

        // 默认给新用户在该项目一个 USER 角色
        AuthUserProjectMembership membership = new AuthUserProjectMembership();
        membership.setUser(saved);
        membership.setProject(project);
        membership.setRole(ProjectRole.USER);
        membershipRepository.save(membership);

        List<String> roles = List.of(membership.getRole().name());

        // access token（带 projectId + roles）
        String accessToken = jwtService.generateAccessToken(
                saved.getId(),
                saved.getEmail(),
                projectId,
                roles
        );

        // refresh token（长生命周期）
        String refreshToken = refreshTokenService.generateAndStore(
                saved.getId(),
                projectId,
                userAgent,
                ip
        );

        return new AuthTokensDTO(accessToken, refreshToken);
    }

    public AuthTokensDTO login(String email,
                               String rawPassword,
                               String projectId,
                               String userAgent,
                               String ip) {

        AuthUser user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        AuthProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        AuthUserProjectMembership membership = membershipRepository
                .findByUserAndProject(user, project)
                .orElseThrow(() -> new ProjectAccessDeniedException(projectId));

        List<String> roles = List.of(membership.getRole().name());

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), projectId, roles
        );

        String refreshToken = refreshTokenService.generateAndStore(
                user.getId(),
                projectId,       // 🔥 重要！
                userAgent,
                ip
        );

        return new AuthTokensDTO(accessToken, refreshToken);
    }


    public AuthUser getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
