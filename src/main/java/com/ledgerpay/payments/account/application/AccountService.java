package com.ledgerpay.payments.account.application;

import com.ledgerpay.common.error.ResourceConflictException;
import com.ledgerpay.common.error.ResourceNotFoundException;
import com.ledgerpay.payments.account.domain.Account;
import com.ledgerpay.payments.account.domain.AccountStatus;
import com.ledgerpay.payments.account.infrastructure.AccountRepository;
import com.ledgerpay.payments.customer.infrastructure.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final String NOT_FOUND_CODE =
            "ACCOUNT_NOT_FOUND";

    private static final String CUSTOMER_NOT_FOUND_CODE =
            "CUSTOMER_NOT_FOUND";

    private static final String NAME_CONFLICT_CODE =
            "ACCOUNT_NAME_CONFLICT";

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public AccountData create(long customerId, String name) {
        requireCustomer(customerId);

        String normalizedName = normalizeName(name);

        if (accountRepository.existsByCustomerIdAndName(
                customerId,
                normalizedName
        )) {
            throw nameConflict(customerId, normalizedName);
        }

        Account account = new Account(
                customerId,
                normalizedName
        );

        try {
            return toData(
                    accountRepository.saveAndFlush(account)
            );
        } catch (DataIntegrityViolationException exception) {
            throw nameConflict(customerId, normalizedName);
        }
    }

    @Transactional(readOnly = true)
    public AccountData get(long id) {
        return toData(findAccount(id));
    }

    @Transactional(readOnly = true)
    public AccountPage list(
            int page,
            int size,
            Long customerId,
            AccountStatus status
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Account> accounts;

        if (customerId != null && status != null) {
            accounts =
                    accountRepository
                            .findAllByCustomerIdAndStatus(
                                    customerId,
                                    status,
                                    pageRequest
                            );
        } else if (customerId != null) {
            accounts =
                    accountRepository.findAllByCustomerId(
                            customerId,
                            pageRequest
                    );
        } else if (status != null) {
            accounts =
                    accountRepository.findAllByStatus(
                            status,
                            pageRequest
                    );
        } else {
            accounts = accountRepository.findAll(pageRequest);
        }

        return new AccountPage(
                accounts.getContent()
                        .stream()
                        .map(AccountService::toData)
                        .toList(),
                accounts.getNumber(),
                accounts.getSize(),
                accounts.getTotalElements(),
                accounts.getTotalPages()
        );
    }

    @Transactional
    public AccountData update(
            long id,
            String name,
            AccountStatus status
    ) {
        Account account = findAccount(id);
        String normalizedName = normalizeName(name);

        if (accountRepository
                .existsByCustomerIdAndNameAndIdNot(
                        account.getCustomerId(),
                        normalizedName,
                        id
                )) {
            throw nameConflict(
                    account.getCustomerId(),
                    normalizedName
            );
        }

        account.update(normalizedName, status);

        try {
            /*
             * Account is already managed by JPA. Calling flush
             * makes Hibernate dirty checking write the update now.
             */
            accountRepository.flush();
            return toData(account);
        } catch (DataIntegrityViolationException exception) {
            throw nameConflict(
                    account.getCustomerId(),
                    normalizedName
            );
        }
    }

    @Transactional
    public void delete(long id) {
        Account account = findAccount(id);
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public boolean hasAccountsForCustomer(long customerId) {
        return accountRepository.existsByCustomerId(customerId);
    }

    private Account findAccount(long id) {
        return accountRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                NOT_FOUND_CODE,
                                "Account %d was not found"
                                        .formatted(id)
                        )
                );
    }

    private void requireCustomer(long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException(
                    CUSTOMER_NOT_FOUND_CODE,
                    "Customer %d was not found"
                            .formatted(customerId)
            );
        }
    }

    private static ResourceConflictException nameConflict(
            long customerId,
            String name
    ) {
        return new ResourceConflictException(
                NAME_CONFLICT_CODE,
                "Customer %d already has an account named %s"
                        .formatted(customerId, name)
        );
    }

    private static String normalizeName(String name) {
        return name.trim();
    }

    private static AccountData toData(Account account) {
        return new AccountData(
                account.getId(),
                account.getCustomerId(),
                account.getName(),
                account.getStatus(),
                account.getCurrency(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}