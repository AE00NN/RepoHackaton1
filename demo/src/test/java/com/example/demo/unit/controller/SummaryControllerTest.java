package com.example.demo.unit.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
//import com.oreo.insight.controller.SummaryController;
//import com.oreo.insight.domain.dto.SummaryRequest;
//import com.oreo.insight.service.SummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SummaryController.class)
public class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SummaryService summaryService;

    @Test
    @WithMockUser(roles = "CENTRAL")
    void shouldRequestWeeklySummary() throws Exception {
        SummaryRequest request = new SummaryRequest();
        request.setFrom(LocalDate.now().minusDays(7));
        request.setTo(LocalDate.now());
        request.setBranch("Miraflores");
        request.setEmailTo("gerente@oreo.com");

        when(summaryService.requestWeeklySummary(any(SummaryRequest.class), any(String.class)))
                .thenReturn("req_12345");

        mockMvc.perform(post("/sales/summary/weekly")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value("req_12345"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "BRANCH", username = "branch_user")
    void shouldPreventBranchUserFromRequestingOtherBranchSummary() throws Exception {
        SummaryRequest request = new SummaryRequest();
        request.setFrom(LocalDate.now().minusDays(7));
        request.setTo(LocalDate.now());
        request.setBranch("San Isidro"); // Different branch
        request.setEmailTo("gerente@oreo.com");

        when(summaryService.requestWeeklySummary(any(SummaryRequest.class), eq("branch_user")))
                .thenThrow(new SecurityException("Cannot generate summary for other branch"));

        mockMvc.perform(post("/sales/summary/weekly")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CENTRAL")
    void shouldValidateEmailInRequest() throws Exception {
        SummaryRequest request = new SummaryRequest();
        request.setFrom(LocalDate.now().minusDays(7));
        request.setTo(LocalDate.now());
        request.setBranch("Miraflores");
        request.setEmailTo("invalid-email"); // Invalid email

        mockMvc.perform(post("/sales/summary/weekly")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}