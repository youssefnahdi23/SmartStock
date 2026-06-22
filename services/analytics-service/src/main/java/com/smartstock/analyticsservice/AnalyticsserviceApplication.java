package com.smartstock.analyticsService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Analytics Service Application
 * 
 * Microservice for managing analytics service
 */
@SpringBootApplication
@EnableKafka
public class AnalyticsserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsserviceApplication.class, args);
    }
}
