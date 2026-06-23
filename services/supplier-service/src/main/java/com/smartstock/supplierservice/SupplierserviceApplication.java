package com.smartstock.supplierService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Supplier Service Application
 * 
 * Microservice for managing supplier service
 */
@SpringBootApplication
@EnableKafka
public class SupplierserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SupplierserviceApplication.class, args);
    }
}
