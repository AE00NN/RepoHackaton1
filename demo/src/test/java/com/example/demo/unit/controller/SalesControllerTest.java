package com.example.demo.unit.controller;


//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.example.demo.unit.controller.SalesController;
//import com.example.demo.unit.domain.dto.SaleRequest;
//import com.example.demo.unit.domain.entity.Sale;
//import com.example.demo.unit.service.SalesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SalesController.class)
public class SalesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SalesService salesService;

    @Test
    @WithMockUser(roles = "CENTRAL")
    void shouldCreateSale() throws Exception {
        SaleRequest request = new SaleRequest();
        request.setSku("OREO_CLASSIC_12");
        request.setUnits(25);
        request.setPrice(1.99);
        request.setBranch("Miraflores");
        request.setSoldAt(LocalDateTime.now());

        Sale mockSale = Sale.builder()
                .id(UUID.randomUUID().toString())
                .sku("OREO_CLASSIC_12")
                .units(25)
                .price(1.99)
                .branch("Miraflores")
                .soldAt(LocalDateTime.now())
                .createdBy("test_user")
                .build();

        when(salesService.createSale(any(SaleRequest.class), any(String.class))).thenReturn(mockSale);

        mockMvc.perform(post("/sales")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value("OREO_CLASSIC_12"))
                .andExpect(jsonPath("$.branch").value("Miraflores"));
    }

    @Test
    @WithMockUser(roles = "BRANCH", username = "branch_user")
    void shouldPreventBranchUserFromCreatingSaleForOtherBranch() throws Exception {
        SaleRequest request = new SaleRequest();
        request.setSku("OREO_CLASSIC_12");
        request.setUnits(25);
        request.setPrice(1.99);
        request.setBranch("San Isidro"); // Different from user's branch

        when(salesService.createSale(any(SaleRequest.class), eq("branch_user")))
                .thenThrow(new SecurityException("Cannot create sale for other branch"));

        mockMvc.perform(post("/sales")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CENTRAL")
    void shouldGetSaleById() throws Exception {
        String saleId = "sale_123";
        Sale mockSale = Sale.builder()
                .id(saleId)
                .sku("OREO_CLASSIC_12")
                .units(25)
                .price(1.99)
                .branch("Miraflores")
                .build();

        when(salesService.getSaleById(saleId, "user")).thenReturn(mockSale);

        mockMvc.perform(get("/sales/{id}", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saleId))
                .andExpect(jsonPath("$.sku").value("OREO_CLASSIC_12"));
    }
}