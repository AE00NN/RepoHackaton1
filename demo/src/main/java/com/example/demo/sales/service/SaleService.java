
package com.example.demo.sales.service;

import com.example.demo.sales.dto.CreateSaleRequestDTO;
import com.example.demo.sales.dto.UpdateSaleRequestDTO;
import com.example.demo.sales.model.Sale;
import com.example.demo.sales.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleService {

    private final SaleRepository saleRepository;

    @Transactional
    public Sale create(CreateSaleRequestDTO request, String username, String userBranch) {
        if (userBranch != null && !userBranch.equals(request.getBranch())) {
            throw new AccessDeniedException(
                    "No tiene permisos para crear ventas en la sucursal: " + request.getBranch()
            );
        }

        Sale sale = Sale.builder()
                .sku(request.getSku())
                .units(request.getUnits())
                .price(request.getPrice())
                .branch(request.getBranch())
                .soldAt(request.getSoldAt())
                .createdBy(username)
                .build();

        return saleRepository.save(sale);
    }

    public Sale getById(String id, String userBranch) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Venta no encontrada con ID: " + id));

        if (userBranch != null && !userBranch.equals(sale.getBranch())) {
            throw new AccessDeniedException("No tiene permisos para ver esta venta");
        }

        return sale;
    }

    public Page<Sale> list(String branch, Instant from, Instant to,
                           int page, int size, String userBranch) {
        String effectiveBranch = (userBranch != null) ? userBranch : branch;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "soldAt"));
        return saleRepository.findByFilters(effectiveBranch, from, to, pageable);
    }

    @Transactional
    public Sale update(String id, UpdateSaleRequestDTO request, String userBranch) {
        Sale sale = getById(id, userBranch);

        if (userBranch != null && !userBranch.equals(request.getBranch())) {
            throw new AccessDeniedException("No puede cambiar la venta a otra sucursal");
        }

        sale.setSku(request.getSku());
        sale.setUnits(request.getUnits());
        sale.setPrice(request.getPrice());
        sale.setBranch(request.getBranch());
        sale.setSoldAt(request.getSoldAt());

        return saleRepository.save(sale);
    }

    @Transactional
    public void delete(String id) {
        if (!saleRepository.existsById(id)) {
            throw new NoSuchElementException("Venta no encontrada con ID: " + id);
        }
        saleRepository.deleteById(id);
    }
}