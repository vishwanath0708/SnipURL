package com.url_shortner.SnipURL.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor  // ← ADD THIS - Jackson needs it!
@AllArgsConstructor // ← ADD THIS - Required for @Builder
public class ShortenResponse {
    private String shortUrl;
    private String shortCode;
    private String longUrl;
    private Integer clickCount;
    private String createdAt;
    private LocalDateTime expiresAt;
}