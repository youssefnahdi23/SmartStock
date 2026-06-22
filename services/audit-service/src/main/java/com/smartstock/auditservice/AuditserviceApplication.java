package com.smartstock.auditService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Audit Service Application
 * 
 * Microservice for managing audit service
 */
@SpringBootApplication
@EnableKafka
public class AuditserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuditserviceApplication.class, args);
    }
}
