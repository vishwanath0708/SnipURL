package com.url_shortner.SnipURL.controller;

import com.url_shortner.SnipURL.dto.ExpandResponse;
import com.url_shortner.SnipURL.dto.ShortenRequest;
import com.url_shortner.SnipURL.dto.ShortenResponse;
import com.url_shortner.SnipURL.entity.UrlMapping;
import com.url_shortner.SnipURL.exception.UrlExpiredException;
import com.url_shortner.SnipURL.service.ClickTrackingService;
import com.url_shortner.SnipURL.service.UrlService;
import com.url_shortner.SnipURL.exception.UrlNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlService urlService;
    private final ClickTrackingService clickTrackingService;

    @PostMapping("/api/shorten")
    @ResponseBody
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        ShortenResponse response = urlService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/expand/{shortCode}")
    @ResponseBody
    public ResponseEntity<ExpandResponse> expandUrl(@PathVariable String shortCode) {
        try {
            String longUrl = urlService.getLongUrl(shortCode);
            UrlMapping mapping = urlService.getUrlMapping(shortCode);

            ExpandResponse response = ExpandResponse.builder()
                    .shortCode(shortCode)
                    .longUrl(longUrl)
                    .success(true)
                    .message("URL expanded successfully")
                    .expiresAt(mapping.getExpiresAt())
                    .build();

            return ResponseEntity.ok(response);

        } catch (UrlNotFoundException e) {
            ExpandResponse response = ExpandResponse.builder()
                    .shortCode(shortCode)
                    .success(false)
                    .message("URL not found: " + shortCode)
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (UrlExpiredException e) {
            ExpandResponse response = ExpandResponse.builder()
                    .shortCode(shortCode)
                    .success(false)
                    .message("URL expired: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.GONE).body(response);
        }
    }

    /**
     * Redirect endpoint - User clicks short URL, gets redirected to original
     * ✅ FIXED: Click tracking and count increment happen on EVERY request
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToLongUrl(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        log.info("🔄 Redirect request for: {}", shortCode);

        try {
            // ✅ STEP 1: Track click in clicks table (async - always runs)
            clickTrackingService.trackClick(shortCode);

            // ✅ STEP 2: Increment click_count in url_mappings table (always runs)
            urlService.incrementClickCount(shortCode);

            // ✅ STEP 3: Get the URL (uses cache - may or may not hit database)
            String longUrl = urlService.getLongUrl(shortCode);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", longUrl);
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();

        } catch (UrlNotFoundException e) {
            log.error("URL not found: {}", shortCode);
            return ResponseEntity.notFound().build();
        } catch (UrlExpiredException e) {
            log.error("URL expired: {}", shortCode);
            return ResponseEntity.status(HttpStatus.GONE).build();
        } catch (Exception e) {
            log.error("Error redirecting: {}", shortCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}