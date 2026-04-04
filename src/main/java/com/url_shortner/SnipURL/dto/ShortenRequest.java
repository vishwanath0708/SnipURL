package com.url_shortner.SnipURL.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortenRequest {

    @NotBlank(message = "Long URL is required")
    @URL(message = "Invalid URL format. Must start with http:// or https://")
    private String longUrl;

    private Integer expiresIn;  // in hours

    // NEW: Expire at specific date/time
    private LocalDateTime expiresAt;


    private String userId;
}