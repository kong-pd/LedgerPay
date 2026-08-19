package com.ledgerpay.payments.account.api;

import com.ledgerpay.common.api.PageResponse;
import com.ledgerpay.payments.account.application.AccountData;
import com.ledgerpay.payments.account.application.AccountPage;
import com.ledgerpay.payments.account.application.AccountService;
import com.ledgerpay.payments.account.domain.AccountStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountData account = accountService.create(
                request.customerId(),
                request.name()
        );

        URI location = URI.create(
                "/api/v1/accounts/" + account.id()
        );

        return ResponseEntity
                .created(location)
                .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    AccountResponse get(
            @PathVariable @Positive long id
    ) {
        return AccountResponse.from(accountService.get(id));
    }

    @GetMapping
    PageResponse<AccountResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(required = false)
            @Positive
            Long customerId,

            @RequestParam(required = false)
            AccountStatus status
    ) {
        AccountPage accountPage = accountService.list(
                page,
                size,
                customerId,
                status
        );

        return new PageResponse<>(
                accountPage.content()
                        .stream()
                        .map(AccountResponse::from)
                        .toList(),
                accountPage.page(),
                accountPage.size(),
                accountPage.totalElements(),
                accountPage.totalPages()
        );
    }

    @PutMapping("/{id}")
    AccountResponse update(
            @PathVariable @Positive long id,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return AccountResponse.from(
                accountService.update(
                        id,
                        request.name(),
                        request.status()
                )
        );
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @PathVariable @Positive long id
    ) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}