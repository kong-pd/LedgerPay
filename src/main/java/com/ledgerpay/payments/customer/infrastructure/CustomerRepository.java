package com.ledgerpay.payments.customer.infrastructure;

import com.ledgerpay.payments.customer.domain.Customer;
import com.ledgerpay.payments.customer.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Page<Customer> findAllByStatus(
            CustomerStatus status,
            Pageable pageable
    );
}