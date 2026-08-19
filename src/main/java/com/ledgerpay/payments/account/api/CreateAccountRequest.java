package com.ledgerpay.payments.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotNull
        @Positive
        Long customerId,

        @NotBlank
        @Size(max = 100)
        String name
) {
}