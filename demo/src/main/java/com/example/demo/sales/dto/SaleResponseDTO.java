package com.example.demo.sales.dto;

import com.example.demo.sales.model.Sale;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaleResponseDTO {

    private String id;
    private String sku;
    private Integer units;
    private BigDecimal price;
    private String branch;
    private Instant soldAt;
    private String createdBy;

    public static SaleResponseDTO from(Sale sale) {
        return SaleResponseDTO.builder()
                .id(sale.getId())
                .sku(sale.getSku())
                .units(sale.getUnits())
                .price(sale.getPrice())
                .branch(sale.getBranch())
                .soldAt(sale.getSoldAt())
                .createdBy(sale.getCreatedBy())
                .build();
    }
}