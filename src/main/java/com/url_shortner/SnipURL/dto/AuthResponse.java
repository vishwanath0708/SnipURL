package com.url_shortner.SnipURL.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;           // JWT token
    private String type = "Bearer"; // Token type
    private String email;           // User email (not ID!)
    private String name;            // User name
    private String role;            // User role
    private Long expiresAt;         // Token expiry
}