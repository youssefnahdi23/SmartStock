package com.smartstock.customerService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Customer Service Application
 * 
 * Microservice for managing customer service
 */
@SpringBootApplication
@EnableKafka
public class CustomerserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CustomerserviceApplication.class, args);
    }
}
