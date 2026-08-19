package com.ledgerpay.payments.customer.api;

import com.ledgerpay.common.api.PageResponse;
import com.ledgerpay.payments.customer.application.CustomerData;
import com.ledgerpay.payments.customer.application.CustomerPage;
import com.ledgerpay.payments.customer.application.CustomerService;
import com.ledgerpay.payments.customer.domain.CustomerStatus;
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
@RequestMapping("/api/v1/customers")
@Tag(
        name = "Customers",
        description = "Customer lifecycle management"
)
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Create a customer")
    @ApiResponse(
            responseCode = "201",
            description = "Customer created",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CustomerResponse.class)
            )
    )
    ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerData customer = customerService.create(
                request.email(),
                request.fullName()
        );

        URI location = URI.create(
                "/api/v1/customers/" + customer.id()
        );

        return ResponseEntity
                .created(location)
                .body(CustomerResponse.from(customer));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer")
    CustomerResponse get(
            @PathVariable @Positive long id
    ) {
        return CustomerResponse.from(customerService.get(id));
    }

    @GetMapping
    @Operation(summary = "List customers")
    PageResponse<CustomerResponse> list(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size,

            @RequestParam(required = false)
            CustomerStatus status
    ) {
        CustomerPage customerPage =
                customerService.list(page, size, status);

        return new PageResponse<>(
                customerPage.content()
                        .stream()
                        .map(CustomerResponse::from)
                        .toList(),
                customerPage.page(),
                customerPage.size(),
                customerPage.totalElements(),
                customerPage.totalPages()
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a customer")
    CustomerResponse update(
            @PathVariable @Positive long id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return CustomerResponse.from(
                customerService.update(
                        id,
                        request.email(),
                        request.fullName(),
                        request.status()
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer")
    @ApiResponse(responseCode = "204", description = "Customer deleted")
    ResponseEntity<Void> delete(
            @PathVariable @Positive long id
    ) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
