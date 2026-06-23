package com.smartstock.productService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Product Service Application
 * 
 * Microservice for managing product service
 */
@SpringBootApplication
@EnableKafka
public class ProductserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ProductserviceApplication.class, args);
    }
}
