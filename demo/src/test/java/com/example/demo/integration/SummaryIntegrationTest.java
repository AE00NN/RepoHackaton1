package com.example.demo.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class SummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender; // Mock email sending for tests

    private String centralToken;
    private String branchToken;

    @BeforeEach
    void setup() throws Exception {
        // Setup test users
        setupTestUsers();

        // Create some test sales data
        createTestSalesData();
    }

    private void setupTestUsers() throws Exception {
        // Create CENTRAL user
        mockMvc.perform(post("/auth/register")
                .content("""
                    {
                        "username": "summary.central",
                        "email": "summary.central@test.com",
                        "password": "test1234",
                        "role": "ROLE_CENTRAL"
                    }
                    """)
                .contentType(MediaType.APPLICATION_JSON));

        // Create BRANCH user
        mockMvc.perform(post("/auth/register")
                .content("""
                    {
                        "username": "summary.branch",
                        "email": "summary.branch@test.com",
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
                        "username": "summary.central",
                        "password": "test1234"
                    }
                    """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        String branchLogin = mockMvc.perform(post("/auth/login")
                        .content("""
                    {
                        "username": "summary.branch",
                        "password": "test1234"
                    }
                    """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        // Extract tokens (simplified)
        centralToken = "Bearer " + extractToken(centralLogin);
        branchToken = "Bearer " + extractToken(branchLogin);
    }

    private void createTestSalesData() throws Exception {
        // Create sales for different branches and dates
        createSale("OREO_CLASSIC_12", 25, 1.99, "Miraflores", "2025-09-01T10:30:00");
        createSale("OREO_DOUBLE", 40, 2.49, "Miraflores", "2025-09-02T15:10:00");
        createSale("OREO_THINS", 32, 2.19, "San Isidro", "2025-09-03T11:05:00");
        createSale("OREO_DOUBLE", 55, 2.49, "San Isidro", "2025-09-04T18:50:00");
        createSale("OREO_CLASSIC_12", 20, 1.99, "Miraflores", "2025-09-05T09:40:00");
    }

    private void createSale(String sku, int units, double price, String branch, String soldAt) throws Exception {
        String saleJson = String.format("""
            {
                "sku": "%s",
                "units": %d,
                "price": %.2f,
                "branch": "%s",
                "soldAt": "%s"
            }
            """, sku, units, price, branch, soldAt);

        mockMvc.perform(post("/sales")
                .header("Authorization", centralToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saleJson));
    }

    @Test
    void shouldRequestWeeklySummaryAsBranchUser() throws Exception {
        String summaryRequest = """
            {
                "from": "2025-09-01",
                "to": "2025-09-07",
                "branch": "Miraflores",
                "emailTo": "gerente@oreo.com"
            }
            """;

        mockMvc.perform(post("/sales/summary/weekly")
                        .header("Authorization", branchToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(summaryRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.estimatedTime").exists());
    }

    @Test
    void shouldRequestWeeklySummaryAsCentralUser() throws Exception {
        String summaryRequest = """
            {
                "from": "2025-09-01",
                "to": "2025-09-07",
                "branch": "San Isidro",
                "emailTo": "gerente@oreo.com"
            }
            """;

        mockMvc.perform(post("/sales/summary/weekly")
                        .header("Authorization", centralToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(summaryRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void shouldPreventBranchUserFromRequestingOtherBranchSummary() throws Exception {
        String summaryRequest = """
            {
                "from": "2025-09-01",
                "to": "2025-09-07",
                "branch": "San Isidro",
                "emailTo": "gerente@oreo.com"
            }
            """;

        mockMvc.perform(post("/sales/summary/weekly")
                        .header("Authorization", branchToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(summaryRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldValidateEmailInSummaryRequest() throws Exception {
        String summaryRequest = """
            {
                "from": "2025-09-01",
                "to": "2025-09-07",
                "branch": "Miraflores",
                "emailTo": "invalid-email-format"
            }
            """;

        mockMvc.perform(post("/sales/summary/weekly")
                        .header("Authorization", branchToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(summaryRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleSummaryRequestWithoutDates() throws Exception {
        String summaryRequest = """
            {
                "branch": "Miraflores",
                "emailTo": "gerente@oreo.com"
            }
            """;

        mockMvc.perform(post("/sales/summary/weekly")
                        .header("Authorization", branchToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(summaryRequest))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldRejectUnauthorizedSummaryRequest() throws Exception {
        String summaryRequest = """
            {
                "branch": "Miraflores",
                "emailTo": "gerente@oreo.com"
            }
            """;

        // No Authorization header
        mockMvc.perform(post("/sales/summary/weekly")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(summaryRequest))
                .andExpect(status().isForbidden());
    }

    private String extractToken(String response) {
        // Simple extraction - in real scenario parse JSON
        return "mock_token_for_summary_test";
    }
}