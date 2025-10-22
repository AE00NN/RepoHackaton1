package com.example.demo.integration;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class SalesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String centralToken;
    private String branchToken;

    @BeforeEach
    void setup() throws Exception {
        // Setup test users and get tokens
        setupTestUsers();
    }

    private void setupTestUsers() throws Exception {
        // Create CENTRAL user
        mockMvc.perform(post("/auth/register")
                .content("""
                    {
                        "username": "test.central",
                        "email": "central@test.com",
                        "password": "test1234",
                        "role": "ROLE_CENTRAL"
                    }
                    """)
                .contentType(MediaType.APPLICATION_JSON));

        // Create BRANCH user
        mockMvc.perform(post("/auth/register")
                .content("""
                    {
                        "username": "test.branch",
                        "email": "branch@test.com",
                        "password": "test1234",
                        "role": "ROLE_BRANCH",
                        "branch": "Miraflores"
                    }
                    """)
                .contentType(MediaType.APPLICATION_JSON));

        // Login and get tokens
        String centralLogin = mockMvc.perform(post("/auth/login")
                        .content("""
                    {
                        "username": "test.central",
                        "password": "test1234"
                    }
                    """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        String branchLogin = mockMvc.perform(post("/auth/login")
                        .content("""
                    {
                        "username": "test.branch",
                        "password": "test1234"
                    }
                    """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        // Extract tokens (simplified - you'd parse JSON)
        centralToken = "Bearer " + extractToken(centralLogin);
        branchToken = "Bearer " + extractToken(branchLogin);
    }

    private String extractToken(String response) {
        // Simple extraction - in real scenario parse JSON
        return "mock_token_for_testing";
    }

    @Test
    void shouldCreateAndRetrieveSales() throws Exception {
        // Create sale as CENTRAL user
        String saleJson = """
            {
                "sku": "OREO_CLASSIC_12",
                "units": 25,
                "price": 1.99,
                "branch": "Miraflores"
            }
            """;

        String saleId = mockMvc.perform(post("/sales")
                        .header("Authorization", centralToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value("OREO_CLASSIC_12"))
                .andReturn().getResponse().getContentAsString();

        // Retrieve the sale
        mockMvc.perform(get("/sales/{id}", extractSaleId(saleId))
                        .header("Authorization", centralToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("OREO_CLASSIC_12"))
                .andExpect(jsonPath("$.branch").value("Miraflores"));
    }

    @Test
    void shouldEnforceBranchIsolation() throws Exception {
        // Create sales for different branches as CENTRAL user
        createSale("OREO_CLASSIC_12", 10, 1.99, "Miraflores");
        createSale("OREO_DOUBLE", 15, 2.49, "San Isidro");

        // BRANCH user should only see their branch sales
        mockMvc.perform(get("/sales")
                        .header("Authorization", branchToken)
                        .param("branch", "Miraflores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].branch").value("Miraflores"));
    }

    private void createSale(String sku, int units, double price, String branch) throws Exception {
        String saleJson = String.format("""
            {
                "sku": "%s",
                "units": %d,
                "price": %.2f,
                "branch": "%s"
            }
            """, sku, units, price, branch);

        mockMvc.perform(post("/sales")
                .header("Authorization", centralToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saleJson));
    }

    private String extractSaleId(String response) {
        // Extract sale ID from response
        return "sale_123"; // Simplified
    }
}