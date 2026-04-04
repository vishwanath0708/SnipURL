package com.url_shortner.SnipURL.service;

import com.url_shortner.SnipURL.entity.Click;
import com.url_shortner.SnipURL.repository.ClickRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClickTrackingService {

    private final ClickRepository clickRepository;
    private final GeoIpService geoIpService;  // ← ADD THIS

    @Async
    @Transactional
    public void trackClick(String shortCode) {
        log.info("🎯 TRACKING CLICK for: {}", shortCode);

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            Click click = new Click();
            click.setShortCode(shortCode);
            click.setClickedAt(LocalDateTime.now());

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ipAddress = getClientIp(request);
                click.setIpAddress(ipAddress);
                click.setUserAgent(request.getHeader("User-Agent"));
                click.setReferer(request.getHeader("Referer"));

                // ✅ GET REAL COUNTRY FROM IP
                String country = geoIpService.getCountryFromIp(ipAddress);
                click.setCountry(country);

                // Parse device from User-Agent
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null) {
                    if (userAgent.toLowerCase().contains("mobile")) {
                        click.setDevice("Mobile");
                    } else if (userAgent.toLowerCase().contains("tablet")) {
                        click.setDevice("Tablet");
                    } else {
                        click.setDevice("Desktop");
                    }
                }
            } else {
                // No request context - minimal info
                click.setDevice("Unknown");
                click.setCountry("Unknown");
            }

            clickRepository.save(click);
            log.info("✅ Click saved for: {}", shortCode);

        } catch (Exception e) {
            log.error("❌ Failed to track click: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}