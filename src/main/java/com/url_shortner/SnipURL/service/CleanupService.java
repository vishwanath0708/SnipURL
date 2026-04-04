package com.url_shortner.SnipURL.service;

import com.url_shortner.SnipURL.repository.UrlMappingRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final UrlMappingRepo urlRepository;

    // Runs every hour at minute 0
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredUrls() {
        log.info("🧹 Running scheduled cleanup of expired URLs...");
        int deletedCount = urlRepository.deleteAllExpiredUrls();
        if (deletedCount > 0) {
            log.info("✅ Deleted {} expired URLs from database", deletedCount);
        } else {
            log.debug("No expired URLs found");
        }
    }
}