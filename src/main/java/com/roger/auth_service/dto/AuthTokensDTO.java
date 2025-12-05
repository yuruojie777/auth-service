package com.roger.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokensDTO {
    private String accessToken;
    private String refreshToken;
}
