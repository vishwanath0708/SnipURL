package com.url_shortner.SnipURL.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExpandResponse {
    private String shortCode;
    private String longUrl;
    private Boolean success;
    private String message;
    private LocalDateTime expiresAt;
}