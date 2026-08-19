package com.ledgerpay.payments.customer.application;

import com.ledgerpay.common.error.ResourceConflictException;
import com.ledgerpay.common.error.ResourceNotFoundException;
import com.ledgerpay.payments.customer.domain.Customer;
import com.ledgerpay.payments.customer.domain.CustomerStatus;
import com.ledgerpay.payments.customer.infrastructure.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class CustomerService {

    private static final String NOT_FOUND_CODE =
            "CUSTOMER_NOT_FOUND";

    private static final String EMAIL_CONFLICT_CODE =
            "CUSTOMER_EMAIL_CONFLICT";

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerData create(String email, String fullName) {
        String normalizedEmail = normalizeEmail(email);

        if (customerRepository.existsByEmail(normalizedEmail)) {
            throw emailConflict(normalizedEmail);
        }

        Customer customer = new Customer(
                normalizedEmail,
                normalizeName(fullName)
        );

        try {
            return toData(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException exception) {
            throw emailConflict(normalizedEmail);
        }
    }

    @Transactional(readOnly = true)
    public CustomerData get(long id) {
        return toData(findCustomer(id));
    }

    @Transactional(readOnly = true)
    public CustomerPage list(
            int page,
            int size,
            CustomerStatus status
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Customer> customers = status == null
                ? customerRepository.findAll(pageRequest)
                : customerRepository.findAllByStatus(
                status,
                pageRequest
        );

        return new CustomerPage(
                customers.getContent()
                        .stream()
                        .map(CustomerService::toData)
                        .toList(),
                customers.getNumber(),
                customers.getSize(),
                customers.getTotalElements(),
                customers.getTotalPages()
        );
    }

    @Transactional
    public CustomerData update(
            long id,
            String email,
            String fullName,
            CustomerStatus status
    ) {
        Customer customer = findCustomer(id);
        String normalizedEmail = normalizeEmail(email);

        if (customerRepository.existsByEmailAndIdNot(
                normalizedEmail,
                id
        )) {
            throw emailConflict(normalizedEmail);
        }

        customer.update(
                normalizedEmail,
                normalizeName(fullName),
                status
        );

        try {
            return toData(
                    customerRepository.saveAndFlush(customer)
            );
        } catch (DataIntegrityViolationException exception) {
            throw emailConflict(normalizedEmail);
        }
    }

    @Transactional
    public void delete(long id) {
        Customer customer = findCustomer(id);
        customerRepository.delete(customer);
    }

    private Customer findCustomer(long id) {
        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                NOT_FOUND_CODE,
                                "Customer %d was not found"
                                        .formatted(id)
                        )
                );
    }

    private static ResourceConflictException emailConflict(
            String email
    ) {
        return new ResourceConflictException(
                EMAIL_CONFLICT_CODE,
                "A customer with email %s already exists"
                        .formatted(email)
        );
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(String fullName) {
        return fullName.trim();
    }

    private static CustomerData toData(Customer customer) {
        return new CustomerData(
                customer.getId(),
                customer.getEmail(),
                customer.getFullName(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}