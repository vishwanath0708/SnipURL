package com.url_shortner.SnipURL.repository;

import com.url_shortner.SnipURL.entity.Click;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickRepository extends JpaRepository<Click, Long> {

    // Get total clicks for a short code
    long countByShortCode(String shortCode);

    // Get clicks in date range
    @Query("SELECT COUNT(c) FROM Click c WHERE c.shortCode = :shortCode AND c.clickedAt >= :startDate")
    long countByShortCodeAndDateRange(@Param("shortCode") String shortCode,
                                      @Param("startDate") LocalDateTime startDate);

    // Get clicks grouped by date (for chart)
    @Query("SELECT FUNCTION('DATE', c.clickedAt) as date, COUNT(c) as count " +
            "FROM Click c WHERE c.shortCode = :shortCode " +
            "AND c.clickedAt >= :startDate " +
            "GROUP BY FUNCTION('DATE', c.clickedAt) " +
            "ORDER BY date")
    List<Object[]> findClicksByDate(@Param("shortCode") String shortCode,
                                    @Param("startDate") LocalDateTime startDate);

    // Get clicks by country
    @Query("SELECT c.country, COUNT(c) FROM Click c " +
            "WHERE c.shortCode = :shortCode AND c.country IS NOT NULL " +
            "GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> findTopCountries(@Param("shortCode") String shortCode);

    // Get clicks by device
    @Query("SELECT c.device, COUNT(c) FROM Click c " +
            "WHERE c.shortCode = :shortCode AND c.device IS NOT NULL " +
            "GROUP BY c.device")
    List<Object[]> findDeviceBreakdown(@Param("shortCode") String shortCode);
}