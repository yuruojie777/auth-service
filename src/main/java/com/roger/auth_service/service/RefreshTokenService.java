package com.roger.auth_service.service;

import com.roger.auth_service.entity.AuthRefreshToken;
import com.roger.auth_service.exception.InvalidRefreshTokenException;
import com.roger.auth_service.repo.AuthRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final AuthRefreshTokenRepository repo;

    private static final long REFRESH_TOKEN_VALIDITY_SECONDS = 60L * 60 * 24 * 30; // 30 天

    public String generateAndStore(UUID userId, String projectId, String userAgent, String ip) {

        String plainToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String hashed = DigestUtils.md5DigestAsHex(plainToken.getBytes(StandardCharsets.UTF_8));

        AuthRefreshToken rt = new AuthRefreshToken();
        rt.setUserId(userId);
        rt.setProjectId(projectId);   // 🔥 加这里
        rt.setTokenHash(hashed);
        rt.setExpiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_VALIDITY_SECONDS));
        rt.setUserAgent(userAgent);
        rt.setIpAddress(ip);

        repo.save(rt);

        return plainToken;
    }

    public AuthRefreshToken consume(String plainToken, String projectId) {
        String hashed = DigestUtils.md5DigestAsHex(plainToken.getBytes(StandardCharsets.UTF_8));

        AuthRefreshToken rt = repo.findByTokenHash(hashed)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (!rt.getProjectId().equals(projectId)) {
            throw new InvalidRefreshTokenException("Refresh token belongs to a different project");
        }

        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired or revoked");
        }

        // 🔥 核心：在这里把旧 token 标记为 revoked / consumed
        rt.setRevoked(true);
        rt.setRevokedAt(Instant.now());
        repo.save(rt);

        return rt;
    }


    public void revokeAll(UUID userId) {
        repo.findAll().stream()
                .filter(rt -> rt.getUserId().equals(userId))
                .forEach(rt -> {
                    rt.setRevoked(true);
                    repo.save(rt);
                });
    }
}
