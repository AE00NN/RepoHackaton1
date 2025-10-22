package com.example.demo.unit.service;


//import com.oreo.insight.domain.dto.SaleRequest;
//import com.oreo.insight.domain.entity.Sale;
//import com.oreo.insight.domain.entity.User;
//import com.oreo.insight.domain.enums.UserRole;
//import com.oreo.insight.domain.repository.SalesRepository;
//import com.oreo.insight.domain.repository.UserRepository;
//import com.oreo.insight.service.SalesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalesServiceTest {

    @Mock
    private SalesRepository salesRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SalesService salesService;

    @Test
    void shouldCreateSaleForCentralUser() {
        // Given
        SaleRequest request = new SaleRequest();
        request.setSku("OREO_CLASSIC_12");
        request.setUnits(25);
        request.setPrice(1.99);
        request.setBranch("Miraflores");
        request.setSoldAt(LocalDateTime.now());

        User centralUser = User.builder()
                .username("central.user")
                .role(UserRole.ROLE_CENTRAL)
                .build();

        when(userRepository.findByUsername("central.user")).thenReturn(Optional.of(centralUser));
        when(salesRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Sale result = salesService.createSale(request, "central.user");

        // Then
        assertThat(result.getSku()).isEqualTo("OREO_CLASSIC_12");
        assertThat(result.getBranch()).isEqualTo("Miraflores");
        assertThat(result.getCreatedBy()).isEqualTo("central.user");
        verify(salesRepository).save(any(Sale.class));
    }

    @Test
    void shouldCreateSaleForBranchUserInTheirBranch() {
        // Given
        SaleRequest request = new SaleRequest();
        request.setSku("OREO_CLASSIC_12");
        request.setUnits(25);
        request.setPrice(1.99);
        request.setBranch("Miraflores");
        request.setSoldAt(LocalDateTime.now());

        User branchUser = User.builder()
                .username("branch.user")
                .role(UserRole.ROLE_BRANCH)
                .branch("Miraflores")
                .build();

        when(userRepository.findByUsername("branch.user")).thenReturn(Optional.of(branchUser));
        when(salesRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Sale result = salesService.createSale(request, "branch.user");

        // Then
        assertThat(result.getSku()).isEqualTo("OREO_CLASSIC_12");
        assertThat(result.getBranch()).isEqualTo("Miraflores");
        assertThat(result.getCreatedBy()).isEqualTo("branch.user");
    }

    @Test
    void shouldPreventBranchUserFromCreatingSaleInOtherBranch() {
        // Given
        SaleRequest request = new SaleRequest();
        request.setSku("OREO_CLASSIC_12");
        request.setUnits(25);
        request.setPrice(1.99);
        request.setBranch("San Isidro"); // Different from user's branch
        request.setSoldAt(LocalDateTime.now());

        User branchUser = User.builder()
                .username("branch.user")
                .role(UserRole.ROLE_BRANCH)
                .branch("Miraflores") // User's branch is Miraflores
                .build();

        when(userRepository.findByUsername("branch.user")).thenReturn(Optional.of(branchUser));

        // When & Then
        assertThatThrownBy(() -> salesService.createSale(request, "branch.user"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Cannot create sale for other branch");
    }

    @Test
    void shouldGetSaleForAuthorizedUser() {
        // Given
        String saleId = "sale_123";
        Sale sale = Sale.builder()
                .id(saleId)
                .sku("OREO_CLASSIC_12")
                .branch("Miraflores")
                .createdBy("branch.user")
                .build();

        User branchUser = User.builder()
                .username("branch.user")
                .role(UserRole.ROLE_BRANCH)
                .branch("Miraflores")
                .build();

        when(salesRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(userRepository.findByUsername("branch.user")).thenReturn(Optional.of(branchUser));

        // When
        Sale result = salesService.getSaleById(saleId, "branch.user");

        // Then
        assertThat(result.getId()).isEqualTo(saleId);
        assertThat(result.getSku()).isEqualTo("OREO_CLASSIC_12");
    }

    @Test
    void shouldPreventBranchUserFromAccessingOtherBranchSale() {
        // Given
        String saleId = "sale_123";
        Sale sale = Sale.builder()
                .id(saleId)
                .sku("OREO_CLASSIC_12")
                .branch("San Isidro") // Different from user's branch
                .createdBy("other.user")
                .build();

        User branchUser = User.builder()
                .username("branch.user")
                .role(UserRole.ROLE_BRANCH)
                .branch("Miraflores") // User's branch is Miraflores
                .build();

        when(salesRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(userRepository.findByUsername("branch.user")).thenReturn(Optional.of(branchUser));

        // When & Then
        assertThatThrownBy(() -> salesService.getSaleById(saleId, "branch.user"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Cannot access sale from other branch");
    }
}