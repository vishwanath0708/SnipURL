package com.url_shortner.SnipURL.service;

import com.url_shortner.SnipURL.dto.ShortenRequest;
import com.url_shortner.SnipURL.dto.ShortenResponse;
import com.url_shortner.SnipURL.entity.UrlMapping;
import com.url_shortner.SnipURL.entity.User;
import com.url_shortner.SnipURL.exception.UrlExpiredException;
import com.url_shortner.SnipURL.repository.UrlMappingRepo;
import com.url_shortner.SnipURL.exception.UrlNotFoundException;
import com.url_shortner.SnipURL.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    private final UrlMappingRepo urlRepository;
    private final UserRepository userRepository;
    private final ClickTrackingService clickTrackingService;
    private final HttpServletRequest request;

    /**
     * Create short URL - Stores in DB and Redis cache
     */
    @CachePut(value = "urls", key = "#result.shortCode")
    @Transactional
    public ShortenResponse createShortUrl(ShortenRequest request) {
        log.info("Creating short URL for: {}", request.getLongUrl());

        // Check if URL already exists
        var existing = urlRepository.findByLongUrl(request.getLongUrl());
        if (existing.isPresent()) {
            log.info("URL already shortened: {}", existing.get().getShortCode());
            return buildResponse(existing.get());
        }

        LocalDateTime expiresAt = null;

        // Check expiration
        if (request.getExpiresAt() != null) {
            expiresAt = request.getExpiresAt();
            log.info("URL will expire at: {}", expiresAt);
        } else if (request.getExpiresIn() != null && request.getExpiresIn() > 0) {
            expiresAt = LocalDateTime.now().plusHours(request.getExpiresIn());
            log.info("URL will expire in {} hours at: {}", request.getExpiresIn(), expiresAt);
        }

        // Generate random unique code
        String shortCode = generateUniqueCode();

        // Create and save mapping
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setLongUrl(request.getLongUrl());
        mapping.setClickCount(0);
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setExpiresAt(expiresAt);

        // Get current user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                mapping.setUser(user);
                log.info("✅ URL associated with user: {}", email);
            }
        }

        UrlMapping saved = urlRepository.save(mapping);
        log.info("Created short URL: {} -> {}", shortCode, request.getLongUrl());

        return buildResponse(saved);
    }

    /**
     * Get original URL - Uses Redis cache
     * NOTE: This method does NOT update click count anymore
     */
    @Cacheable(value = "url_mappings", key = "#shortCode")
    @Transactional
    public String getLongUrl(String shortCode) {
        // This method only runs on CACHE MISS
        log.info("💾 CACHE MISS - Loading from database: {}", shortCode);

        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));

        // Check if expired
        if (mapping.getExpiresAt() != null &&
                LocalDateTime.now().isAfter(mapping.getExpiresAt())) {
            log.warn("⚠️ Expired URL accessed: {}", shortCode);
            throw new UrlExpiredException("This link expired on: " + mapping.getExpiresAt());
        }

        // ✅ REMOVED: clickTrackingService.trackClick(shortCode) - moved to controller
        // ✅ REMOVED: urlRepository.incrementClickCount(shortCode) - moved to controller

        return mapping.getLongUrl();
    }

    /**
     * Increment click count - Called on EVERY request (even cache hits)
     */
    @Transactional
    public void incrementClickCount(String shortCode) {
        urlRepository.incrementClickCount(shortCode);
        log.debug("📊 Click count incremented for: {}", shortCode);
    }

    /**
     * Generate random 6-character unique code
     */
    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    /**
     * Convert Entity to Response DTO
     */
    private ShortenResponse buildResponse(UrlMapping mapping) {
        return ShortenResponse.builder()
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .shortCode(mapping.getShortCode())
                .longUrl(mapping.getLongUrl())
                .expiresAt(mapping.getExpiresAt())
                .clickCount(mapping.getClickCount())
                .createdAt(mapping.getCreatedAt() != null ?
                        mapping.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    public UrlMapping getUrlMapping(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));
    }
}