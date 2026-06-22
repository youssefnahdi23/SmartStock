package com.smartstock.reportingService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Reporting Service Application
 * 
 * Microservice for managing reporting service
 */
@SpringBootApplication
@EnableKafka
public class ReportingserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReportingserviceApplication.class, args);
    }
}
