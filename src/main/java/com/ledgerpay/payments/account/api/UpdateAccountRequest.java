package com.ledgerpay.payments.account.api;

import com.ledgerpay.payments.account.domain.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        AccountStatus status
) {
}