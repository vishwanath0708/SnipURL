package com.url_shortner.SnipURL.controller;

import com.url_shortner.SnipURL.entity.UrlMapping;
import com.url_shortner.SnipURL.entity.User;
import com.url_shortner.SnipURL.repository.UrlMappingRepo;
import com.url_shortner.SnipURL.repository.UserRepository;
import com.url_shortner.SnipURL.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserUrlController {

    private final UrlMappingRepo urlRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/urls")
    public ResponseEntity<List<UrlMapping>> getUserUrls(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Fetching user URLs");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("No valid auth header");
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.getEmailFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        List<UrlMapping> urls = urlRepository.findByUser(user);
        return ResponseEntity.ok(urls);
    }
}