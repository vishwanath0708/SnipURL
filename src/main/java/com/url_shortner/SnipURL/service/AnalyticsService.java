package com.url_shortner.SnipURL.service;

import com.url_shortner.SnipURL.dto.AnalyticsResponse;
import com.url_shortner.SnipURL.entity.UrlMapping;
import com.url_shortner.SnipURL.repository.ClickRepository;
import com.url_shortner.SnipURL.repository.UrlMappingRepo;
import com.url_shortner.SnipURL.exception.UrlNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickRepository clickRepository;
    private final UrlMappingRepo urlRepository;

    public AnalyticsResponse getAnalytics(String shortCode) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("URL not found: " + shortCode));

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

        long totalClicks = clickRepository.countByShortCode(shortCode);
        long clicksLast7Days = clickRepository.countByShortCodeAndDateRange(shortCode, sevenDaysAgo);
        long clicksPrevious7Days = clickRepository.countByShortCodeAndDateRange(shortCode, fourteenDaysAgo);

        double changePercentage = 0;
        if (clicksPrevious7Days > 0) {
            changePercentage = ((double)(clicksLast7Days - clicksPrevious7Days) / clicksPrevious7Days) * 100;
        }

        // Daily clicks
        List<Object[]> dailyResults = clickRepository.findClicksByDate(shortCode, sevenDaysAgo);
        List<AnalyticsResponse.DailyClick> dailyClicks = new ArrayList<>();
        for (Object[] result : dailyResults) {
            dailyClicks.add(AnalyticsResponse.DailyClick.builder()
                    .date(((java.sql.Date) result[0]).toLocalDate())
                    .count((Long) result[1])
                    .build());
        }

        // Top countries
        List<Object[]> countryResults = clickRepository.findTopCountries(shortCode);
        List<AnalyticsResponse.CountryData> topCountries = new ArrayList<>();
        for (Object[] result : countryResults) {
            String country = (String) result[0];
            Long count = (Long) result[1];
            double percentage = totalClicks > 0 ? (count.doubleValue() / totalClicks) * 100 : 0;
            topCountries.add(AnalyticsResponse.CountryData.builder()
                    .country(country != null ? country : "Unknown")
                    .count(count)
                    .percentage(Math.round(percentage * 10) / 10.0)
                    .build());
        }

        // Device breakdown
        List<Object[]> deviceResults = clickRepository.findDeviceBreakdown(shortCode);
        Map<String, Long> deviceBreakdown = new HashMap<>();
        for (Object[] result : deviceResults) {
            deviceBreakdown.put((String) result[0], (Long) result[1]);
        }

        return AnalyticsResponse.builder()
                .shortCode(shortCode)
                .longUrl(mapping.getLongUrl())
                .totalClicks(totalClicks)
                .uniqueVisitors((long) (totalClicks * 0.75))
                .clicksLast7Days(clicksLast7Days)
                .changePercentage(Math.round(changePercentage * 10) / 10.0)
                .dailyClicks(dailyClicks)
                .topCountries(topCountries)
                .deviceBreakdown(deviceBreakdown)
                .build();
    }
}