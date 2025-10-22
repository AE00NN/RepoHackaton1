package com.example.demo.sales.service;

import com.example.demo.sales.dto.SalesAggregatesDTO;
import com.example.demo.sales.model.Sale;
import com.example.demo.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesAggregationService {

    private final SaleRepository saleRepository;

    public SalesAggregatesDTO calculateAggregates(Instant from, Instant to, String branch) {
        List<Sale> sales = saleRepository.findRangeForAggregates(from, to, branch);

        if (sales.isEmpty()) {
            return SalesAggregatesDTO.builder()
                    .totalUnits(0)
                    .totalRevenue(BigDecimal.ZERO)
                    .topSku("N/A")
                    .topBranch("N/A")
                    .salesCount(0)
                    .build();
        }

        int totalUnits = sales.stream()
                .mapToInt(Sale::getUnits)
                .sum();

        BigDecimal totalRevenue = sales.stream()
                .map(sale -> sale.getPrice().multiply(BigDecimal.valueOf(sale.getUnits())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String topSku = sales.stream()
                .collect(Collectors.groupingBy(Sale::getSku, Collectors.summingInt(Sale::getUnits)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String topBranch = sales.stream()
                .collect(Collectors.groupingBy(Sale::getBranch, Collectors.summingInt(Sale::getUnits)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return SalesAggregatesDTO.builder()
                .totalUnits(totalUnits)
                .totalRevenue(totalRevenue)
                .topSku(topSku)
                .topBranch(topBranch)
                .salesCount(sales.size())
                .build();
    }
}