package com.example.demo.sales.repository;

import com.example.demo.sales.model.Sale;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, String> {

    @Query("""
        SELECT s FROM Sale s
        WHERE (:branch IS NULL OR s.branch = :branch)
          AND (:from IS NULL OR s.soldAt >= :from)
          AND (:to IS NULL OR s.soldAt <= :to)
    """)
    Page<Sale> findByFilters(
            @Param("branch") String branch,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("""
        SELECT s FROM Sale s
        WHERE (:branch IS NULL OR s.branch = :branch)
          AND s.soldAt BETWEEN :from AND :to
    """)
    List<Sale> findRangeForAggregates(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("branch") String branch
    );
}