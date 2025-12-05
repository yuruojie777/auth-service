package com.roger.auth_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {
    /**
     * HS256 secret key, Base64 或普通字符串都可以（demo 用普通字符串）
     */
    private String secret;

    /**
     * access token 有效期（秒）
     */
    private long accessTokenValiditySeconds;
}
