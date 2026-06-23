package com.smartstock.orderService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Order Service Application
 * 
 * Microservice for managing order service
 */
@SpringBootApplication
@EnableKafka
public class OrderserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderserviceApplication.class, args);
    }
}
