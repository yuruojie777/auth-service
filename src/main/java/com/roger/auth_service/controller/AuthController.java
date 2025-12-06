package com.roger.auth_service.controller;

import com.roger.auth_service.dto.*;
import com.roger.auth_service.entity.AuthProject;
import com.roger.auth_service.entity.AuthRefreshToken;
import com.roger.auth_service.entity.AuthUser;
import com.roger.auth_service.entity.AuthUserProjectMembership;
import com.roger.auth_service.exception.InvalidRefreshTokenException;
import com.roger.auth_service.exception.ProjectAccessDeniedException;
import com.roger.auth_service.exception.ProjectNotFoundException;
import com.roger.auth_service.repo.AuthProjectRepository;
import com.roger.auth_service.repo.AuthUserProjectMembershipRepository;
import com.roger.auth_service.service.AuthService;
import com.roger.auth_service.service.JwtService;
import com.roger.auth_service.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.roger.auth_service.annotation.ClientIp;

import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final JwtService jwtService;
        private final RefreshTokenService refreshTokenService;
        private final AuthUserProjectMembershipRepository membershipRepository;
        private final AuthProjectRepository projectRepository;

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<AuthTokensDTO>> register(
                        @Valid @RequestBody RegisterRequestDTO req,
                        @RequestHeader(value = "User-Agent", required = false) String ua,
                        @ClientIp String clientIp) {
                log.info("Register attempt email={} projectId={}", req.getEmail(), req.getProjectId());

                AuthTokensDTO tokens = authService.register(
                                req.getEmail(),
                                req.getPassword(),
                                req.getProjectId(),
                                ua,
                                clientIp);

                return ResponseEntity.ok(ApiResponse.ok(tokens));
        }

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<AuthTokensDTO>> login(
                        @Valid @RequestBody LoginRequestDTO req,
                        @RequestHeader(value = "User-Agent", required = false) String ua,
                        @ClientIp String clientIp) {
                log.info("Login attempt email={} projectId={}", req.getEmail(), req.getProjectId());

                AuthTokensDTO tokens = authService.login(
                                req.getEmail(),
                                req.getPassword(),
                                req.getProjectId(),
                                ua,
                                clientIp);

                return ResponseEntity.ok(ApiResponse.ok(tokens));
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<AuthUser>> me(@AuthenticationPrincipal AuthUser user) {
                return ResponseEntity.ok(ApiResponse.ok(user));
        }

        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<AuthTokensDTO>> refresh(
                        @RequestBody RefreshRequestDTO req,
                        @CookieValue(name = "refresh_token", required = false) String refreshToken,
                        @RequestHeader(value = "User-Agent", required = false) String ua,
                        @ClientIp String clientIp) {
                if (refreshToken == null || refreshToken.isBlank()) {
                        throw new InvalidRefreshTokenException("Missing refresh token cookie");
                }

                // 1. 从 cookie 里的 refresh_token 做 consume（校验 + revoked）
                AuthRefreshToken rt = refreshTokenService.consume(refreshToken, req.getProjectId());

                // 2. 查用户/项目/角色
                AuthUser user = authService.getUserById(rt.getUserId());

                AuthProject project = projectRepository.findById(rt.getProjectId())
                                .orElseThrow(() -> new ProjectNotFoundException(rt.getProjectId()));

                AuthUserProjectMembership membership = membershipRepository
                                .findByUserAndProject(user, project)
                                .orElseThrow(() -> new ProjectAccessDeniedException(rt.getProjectId()));

                List<String> roles = List.of(membership.getRole().name());

                // 3. 生成新的 access token
                String newAccessToken = jwtService.generateAccessToken(
                                user.getId(), user.getEmail(), rt.getProjectId(), roles);

                // 4. 生成新的 refresh token，并写回 Cookie（HttpOnly）
                String newRefreshToken = refreshTokenService.generateAndStore(
                                user.getId(),
                                rt.getProjectId(),
                                ua,
                                clientIp);

                ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                                .httpOnly(true)
                                .secure(true) // dev 可以先 false，生产必须 true
                                .sameSite("Lax") // 或 "Strict"
                                .path("/auth") // 只在 /auth 下发送
                                .maxAge(Duration.ofDays(30))
                                .build();

                // 5. 把新的 access token 放在 body，新的 refresh token 只放 cookie，不再给前端
                AuthTokensDTO dto = new AuthTokensDTO(newAccessToken, null);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(ApiResponse.ok(dto));
        }

}
