package com.smartstock.warehouseService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Warehouse Service Application
 * 
 * Microservice for managing warehouse service
 */
@SpringBootApplication
@EnableKafka
public class WarehouseserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WarehouseserviceApplication.class, args);
    }
}
