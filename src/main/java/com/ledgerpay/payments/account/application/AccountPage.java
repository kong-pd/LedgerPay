package com.ledgerpay.payments.account.application;

import java.util.List;

public record AccountPage(
        List<AccountData> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}