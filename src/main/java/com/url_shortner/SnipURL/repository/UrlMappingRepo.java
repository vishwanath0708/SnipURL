package com.url_shortner.SnipURL.repository;

import com.url_shortner.SnipURL.entity.UrlMapping;
import com.url_shortner.SnipURL.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepo extends JpaRepository<UrlMapping,Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);

    // Find by long URL (used to check if a URL is already shortened)
    Optional<UrlMapping> findByLongUrl(String longUrl);

    // Increment click count without loading the entire entity
    @Modifying
    @Transactional
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    // Check if a short code already exists (for uniqueness)
    boolean existsByShortCode(String shortCode);

    List<UrlMapping> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM UrlMapping u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < CURRENT_TIMESTAMP")
    int deleteAllExpiredUrls();
}