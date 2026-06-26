package com.smartstock.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Notification Service Application
 * 
 * Microservice for managing notification service
 */
@SpringBootApplication
@EnableKafka
public class NotificationserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(NotificationserviceApplication.class, args);
    }
}
