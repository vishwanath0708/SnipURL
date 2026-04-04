package com.url_shortner.SnipURL.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
@Slf4j
public class GeoIpService {

    // Free API - ip-api.com (no API key required, 45 requests/minute)
    private static final String GEOIP_API = "http://ip-api.com/json/%s?fields=country";

    public String getCountryFromIp(String ipAddress) {
        try {
            // Skip local IPs
            if (ipAddress.startsWith("192.168") ||
                    ipAddress.startsWith("10.") ||
                    ipAddress.startsWith("127.") ||
                    ipAddress.equals("0:0:0:0:0:0:0:1")) {
                return "Local";
            }

            // Call free GeoIP API
            String apiUrl = String.format(GEOIP_API, ipAddress);
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON response (simple parsing without library)
            String json = response.toString();
            if (json.contains("\"country\":\"")) {
                int start = json.indexOf("\"country\":\"") + 11;
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            }

            return "Unknown";
        } catch (Exception e) {
            log.debug("Could not get country for IP: {}", ipAddress);
            return "Unknown";
        }
    }
}