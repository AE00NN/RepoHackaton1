package com.example.demo.utils;


import java.time.LocalDateTime;
import java.util.UUID;

public class TestDataBuilder {

    public static User.UserBuilder centralUser() {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .username("central.user")
                .email("central@oreo.com")
                .password("encoded_password")
                .role(UserRole.ROLE_CENTRAL)
                .branch(null);
    }

    public static User.UserBuilder branchUser() {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .username("branch.user")
                .email("branch@oreo.com")
                .password("encoded_password")
                .role(UserRole.ROLE_BRANCH)
                .branch("Miraflores");
    }

    public static Sale.SaleBuilder sale() {
        return Sale.builder()
                .id(UUID.randomUUID().toString())
                .sku("OREO_CLASSIC_12")
                .units(25)
                .price(1.99)
                .branch("Miraflores")
                .soldAt(LocalDateTime.now())
                .createdBy("test_user");
    }

    public static SaleRequest.SaleRequestBuilder saleRequest() {
        SaleRequest request = new SaleRequest();
        request.setSku("OREO_CLASSIC_12");
        request.setUnits(25);
        request.setPrice(1.99);
        request.setBranch("Miraflores");
        request.setSoldAt(LocalDateTime.now());
        return SaleRequest.SaleRequestBuilder.create(request);
    }

    // Builder pattern for SaleRequest
    public static class SaleRequestBuilder {
        private final SaleRequest request;

        private SaleRequestBuilder(SaleRequest request) {
            this.request = request;
        }

        public static SaleRequestBuilder create(SaleRequest request) {
            return new SaleRequestBuilder(request);
        }

        public SaleRequestBuilder withBranch(String branch) {
            request.setBranch(branch);
            return this;
        }

        public SaleRequestBuilder withSku(String sku) {
            request.setSku(sku);
            return this;
        }

        public SaleRequest build() {
            return request;
        }
    }
}