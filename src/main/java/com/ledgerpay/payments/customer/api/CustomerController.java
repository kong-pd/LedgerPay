package com.ledgerpay.payments.customer.api;

import com.ledgerpay.payments.customer.application.CustomerData;
import com.ledgerpay.payments.customer.application.CustomerPage;
import com.ledgerpay.payments.customer.application.CustomerService;
import com.ledgerpay.payments.customer.domain.CustomerStatus;
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
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
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
    CustomerResponse get(
            @PathVariable @Positive long id
    ) {
        return CustomerResponse.from(customerService.get(id));
    }

    @GetMapping
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
    ResponseEntity<Void> delete(
            @PathVariable @Positive long id
    ) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}