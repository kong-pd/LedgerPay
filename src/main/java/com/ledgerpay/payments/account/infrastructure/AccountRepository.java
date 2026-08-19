package com.ledgerpay.payments.account.infrastructure;

import com.ledgerpay.payments.account.domain.Account;
import com.ledgerpay.payments.account.domain.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    boolean existsByCustomerIdAndName(
            Long customerId,
            String name
    );

    boolean existsByCustomerIdAndNameAndIdNot(
            Long customerId,
            String name,
            Long id
    );

    boolean existsByCustomerId(Long customerId);

    Page<Account> findAllByCustomerId(
            Long customerId,
            Pageable pageable
    );

    Page<Account> findAllByStatus(
            AccountStatus status,
            Pageable pageable
    );

    Page<Account> findAllByCustomerIdAndStatus(
            Long customerId,
            AccountStatus status,
            Pageable pageable
    );
}