package com.url_shortner.SnipURL.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "home";  // Returns home.html
    }

    @GetMapping("/login")
    public String login() {
        return "login";  // Returns login.html
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";  // Returns dashboard.html
    }
}