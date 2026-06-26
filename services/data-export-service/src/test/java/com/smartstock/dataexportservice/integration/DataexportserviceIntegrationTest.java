package com.smartstock.dataexportservice.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Data Export Service
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integration Tests")
class DataexportserviceIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("Health endpoint should return UP status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
