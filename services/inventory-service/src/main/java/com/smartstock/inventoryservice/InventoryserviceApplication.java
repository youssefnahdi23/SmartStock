package com.smartstock.inventoryService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Inventory Service Application
 * 
 * Microservice for managing inventory service
 */
@SpringBootApplication
@EnableKafka
public class InventoryserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(InventoryserviceApplication.class, args);
    }
}
