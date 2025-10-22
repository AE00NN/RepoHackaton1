package com.example.demo.sales.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateSaleRequestDTO {

    @NotBlank(message = "SKU es obligatorio")
    private String sku;

    @NotNull(message = "Unidades son obligatorias")
    @Min(value = 1)
    private Integer units;

    @NotNull(message = "Precio es obligatorio")
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @NotBlank(message = "Sucursal es obligatoria")
    private String branch;

    @NotNull(message = "Fecha de venta es obligatoria")
    private Instant soldAt;
}