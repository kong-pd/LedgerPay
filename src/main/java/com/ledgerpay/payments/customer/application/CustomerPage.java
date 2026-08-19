package com.ledgerpay.payments.customer.application;

import java.util.List;

public record CustomerPage(
        List<CustomerData> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}