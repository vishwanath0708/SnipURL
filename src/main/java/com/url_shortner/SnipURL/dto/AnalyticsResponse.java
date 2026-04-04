package com.url_shortner.SnipURL.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {
    private String shortCode;
    private String longUrl;
    private Long totalClicks;
    private Long uniqueVisitors;
    private Long clicksLast7Days;
    private Double changePercentage;
    private List<DailyClick> dailyClicks;
    private List<CountryData> topCountries;
    private Map<String, Long> deviceBreakdown;

    @Data
    @Builder
    public static class DailyClick {
        private LocalDate date;
        private Long count;
    }

    @Data
    @Builder
    public static class CountryData {
        private String country;
        private Long count;
        private Double percentage;
    }
}