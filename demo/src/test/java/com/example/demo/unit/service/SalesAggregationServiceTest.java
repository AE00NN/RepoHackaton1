package com.example.demo.unit.service;

package com.oreo.insight.service;

import com.example.demo.domain.entity.Sale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesAggregationServiceTest {

    private final SalesRepository salesRepository;

    public SalesAggregates calculateAggregates(LocalDate from, LocalDate to, String branch) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        List<Sale> sales = salesRepository.findBySoldAtBetweenAndBranch(
                start, end, branch
        );

        if (sales.isEmpty()) {
            return SalesAggregates.empty();
        }

        int totalUnits = sales.stream().mapToInt(Sale::getUnits).sum();
        double totalRevenue = sales.stream()
                .mapToDouble(s -> s.getUnits() * s.getPrice())
                .sum();

        // Find top SKU
        String topSku = sales.stream()
                .collect(Collectors.groupingBy(Sale::getSku,
                        Collectors.summingInt(Sale::getUnits)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        // Find top branch
        String topBranch = sales.stream()
                .collect(Collectors.groupingBy(Sale::getBranch,
                        Collectors.summingInt(Sale::getUnits)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return SalesAggregates.builder()
                .totalUnits(totalUnits)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .topSku(topSku)
                .topBranch(topBranch)
                .salesCount(sales.size())
                .build();
    }
}
