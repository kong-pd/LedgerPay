package com.ledgerpay.common.error;

public record ApiFieldError(
        String field,
        String message
) {
}