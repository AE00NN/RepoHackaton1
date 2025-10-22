package com.example.demo.sales.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateSaleRequestDTO {

    @NotBlank(message = "SKU es obligatorio")
    private String sku;

    @NotNull(message = "Unidades son obligatorias")
    @Min(value = 1, message = "Unidades debe ser al menos 1")
    private Integer units;

    @NotNull(message = "Precio es obligatorio")
    @DecimalMin(value = "0.01", message = "Precio debe ser mayor a 0")
    private BigDecimal price;

    @NotBlank(message = "Sucursal es obligatoria")
    private String branch;

    private Instant soldAt;
}