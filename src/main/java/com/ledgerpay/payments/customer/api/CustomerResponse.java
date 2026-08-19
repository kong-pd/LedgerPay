package com.ledgerpay.payments.customer.api;

import com.ledgerpay.payments.customer.application.CustomerData;
import com.ledgerpay.payments.customer.domain.CustomerStatus;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String email,
        String fullName,
        CustomerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    static CustomerResponse from(CustomerData customer) {
        return new CustomerResponse(
                customer.id(),
                customer.email(),
                customer.fullName(),
                customer.status(),
                customer.createdAt(),
                customer.updatedAt()
        );
    }
}