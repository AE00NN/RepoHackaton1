package com.example.demo.integration;


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
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterAndLoginCentralUser() throws Exception {
        // Register CENTRAL user
        String registerJson = """
            {
                "username": "oreo.admin",
                "email": "admin@oreo.com",
                "password": "Oreo1234",
                "role": "ROLE_CENTRAL"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("oreo.admin"))
                .andExpect(jsonPath("$.role").value("ROLE_CENTRAL"));

        // Login
        String loginJson = """
            {
                "username": "oreo.admin",
                "password": "Oreo1234"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_CENTRAL"));
    }

    @Test
    void shouldPreventBranchUserFromAccessingOtherBranchData() throws Exception {
        // Setup: Create two branch users
        String mirafloresUser = """
            {
                "username": "miraflores.user",
                "email": "mira@oreo.com",
                "password": "Oreo1234",
                "role": "ROLE_BRANCH",
                "branch": "Miraflores"
            }
            """;

        String sanIsidroUser = """
            {
                "username": "sanisidro.user",
                "email": "sanisidro@oreo.com",
                "password": "Oreo1234",
                "role": "ROLE_BRANCH",
                "branch": "San Isidro"
            }
            """;

        // Register users
        mockMvc.perform(post("/auth/register").content(mirafloresUser).contentType(MediaType.APPLICATION_JSON));
        mockMvc.perform(post("/auth/register").content(sanIsidroUser).contentType(MediaType.APPLICATION_JSON));

        // Login as Miraflores user
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .content("""
                    {
                        "username": "miraflores.user",
                        "password": "Oreo1234"
                    }
                    """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        // Extract token (simplified)
        String token = "Bearer extracted_token"; // You'd parse this from response

        // Try to create sale for different branch - should fail
        String saleForOtherBranch = """
            {
                "sku": "OREO_CLASSIC_12",
                "units": 25,
                "price": 1.99,
                "branch": "San Isidro"
            }
            """;

        mockMvc.perform(post("/sales")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleForOtherBranch))
                .andExpect(status().isForbidden());
    }
}
