package com.ledgerpay.payments.account.api;

import com.ledgerpay.common.api.PageResponse;
import com.ledgerpay.payments.account.application.AccountData;
import com.ledgerpay.payments.account.application.AccountPage;
import com.ledgerpay.payments.account.application.AccountService;
import com.ledgerpay.payments.account.domain.AccountStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.net.URI;

import org.springframework.http.MediaType;
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
@Tag(
        name = "Accounts",
        description = "Balance-free customer Account management"
)
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Create an account")
    @ApiResponse(
            responseCode = "201",
            description = "Account created",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AccountResponse.class)
            )
    )
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
    @Operation(summary = "Get an account")
    AccountResponse get(
            @PathVariable @Positive long id
    ) {
        return AccountResponse.from(accountService.get(id));
    }

    @GetMapping
    @Operation(summary = "List accounts")
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
    @Operation(summary = "Update an account")
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
    @Operation(summary = "Delete an account")
    @ApiResponse(responseCode = "204", description = "Account deleted")
    ResponseEntity<Void> delete(
            @PathVariable @Positive long id
    ) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
