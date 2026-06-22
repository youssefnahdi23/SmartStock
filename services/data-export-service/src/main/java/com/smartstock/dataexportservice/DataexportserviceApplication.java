package com.smartstock.dataExportService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Data Export Service Application
 * 
 * Microservice for managing data export service
 */
@SpringBootApplication
@EnableKafka
public class DataexportserviceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DataexportserviceApplication.class, args);
    }
}
