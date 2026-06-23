package com.smartstock.customerService.presentation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health Check Controller
 * Provides endpoints for service health verification
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    
    @GetMapping("/status")
    public HealthStatus getStatus() {
        return new HealthStatus("UP", "Service is running");
    }
    
    public record HealthStatus(String status, String message) {}
}
