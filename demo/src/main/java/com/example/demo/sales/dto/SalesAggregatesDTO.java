
package com.example.demo.sales.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesAggregatesDTO {
    private int totalUnits;
    private BigDecimal totalRevenue;
    private String topSku;
    private String topBranch;
    private int salesCount;
}