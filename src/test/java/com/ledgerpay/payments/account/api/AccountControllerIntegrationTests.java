package com.ledgerpay.payments.account.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerpay.payments.account.infrastructure.AccountRepository;
import com.ledgerpay.payments.customer.api.CreateCustomerRequest;
import com.ledgerpay.payments.customer.infrastructure.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8")
                    .withUrlParam("sslMode", "DISABLED");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanDatabase() {
        /*
         * Account must be deleted first because it owns the
         * foreign key referencing Customer.
         */
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void createsAccountForExistingCustomer() throws Exception {
        long customerId = createCustomer(
                "account-owner@example.com",
                "Account Owner"
        );

        String requestBody = objectMapper.writeValueAsString(
                new CreateAccountRequest(
                        customerId,
                        "Primary Wallet"
                )
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        startsWith("/api/v1/accounts/")
                ))
                .andExpect(jsonPath(
                        "$.customerId",
                        is((int) customerId)
                ))
                .andExpect(jsonPath(
                        "$.name",
                        is("Primary Wallet")
                ))
                .andExpect(jsonPath(
                        "$.status",
                        is("ACTIVE")
                ))
                .andExpect(jsonPath(
                        "$.currency",
                        is("MYR")
                ))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void rejectsDuplicateAccountNameForSameCustomer()
            throws Exception {
        long customerId = createCustomer(
                "duplicate-account@example.com",
                "Duplicate Account Owner"
        );

        String requestBody = objectMapper.writeValueAsString(
                new CreateAccountRequest(
                        customerId,
                        "Primary Wallet"
                )
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(
                        "$.code",
                        is("ACCOUNT_NAME_CONFLICT")
                ));
    }

    @Test
    void rejectsBlankAccountName() throws Exception {
        long customerId = createCustomer(
                "validation@example.com",
                "Validation Owner"
        );

        String requestBody = objectMapper.writeValueAsString(
                new CreateAccountRequest(
                        customerId,
                        " "
                )
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        is("VALIDATION_FAILED")
                ))
                .andExpect(jsonPath(
                        "$.fieldErrors[0].field",
                        is("name")
                ));
    }

    @Test
    void rejectsAccountForMissingCustomer() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new CreateAccountRequest(
                        999_999L,
                        "Ghost Wallet"
                )
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(
                        "$.code",
                        is("CUSTOMER_NOT_FOUND")
                ));
    }

    @Test
    void paginatesAndFiltersAccountsByCustomerAndStatus()
            throws Exception {
        long ownerId = createCustomer(
                "filtered-owner@example.com",
                "Filtered Owner"
        );

        long otherOwnerId = createCustomer(
                "other-owner@example.com",
                "Other Owner"
        );

        createAccount(ownerId, "Active Wallet");

        long suspendedAccountId = createAccount(
                ownerId,
                "Suspended Wallet"
        );

        createAccount(otherOwnerId, "Other Wallet");

        mockMvc.perform(put(
                        "/api/v1/accounts/{id}",
                        suspendedAccountId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Suspended Wallet",
                                  "status": "SUSPENDED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/accounts")
                        .param("page", "0")
                        .param("size", "1")
                        .param(
                                "customerId",
                                String.valueOf(ownerId)
                        )
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content",
                        hasSize(1)
                ))
                .andExpect(jsonPath(
                        "$.content[0].id",
                        is((int) suspendedAccountId)
                ))
                .andExpect(jsonPath(
                        "$.content[0].customerId",
                        is((int) ownerId)
                ))
                .andExpect(jsonPath(
                        "$.content[0].status",
                        is("SUSPENDED")
                ))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath(
                        "$.totalElements",
                        is(1)
                ))
                .andExpect(jsonPath(
                        "$.totalPages",
                        is(1)
                ));
    }

    @Test
    void updatesAndDeletesAccount() throws Exception {
        long customerId = createCustomer(
                "account-crud@example.com",
                "Account CRUD Owner"
        );

        long accountId = createAccount(
                customerId,
                "Before Wallet"
        );

        mockMvc.perform(put(
                        "/api/v1/accounts/{id}",
                        accountId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "After Wallet",
                                  "status": "SUSPENDED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.name",
                        is("After Wallet")
                ))
                .andExpect(jsonPath(
                        "$.status",
                        is("SUSPENDED")
                ))
                .andExpect(jsonPath(
                        "$.currency",
                        is("MYR")
                ));

        mockMvc.perform(delete(
                        "/api/v1/accounts/{id}",
                        accountId
                ))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                        "/api/v1/accounts/{id}",
                        accountId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(
                        "$.code",
                        is("ACCOUNT_NOT_FOUND")
                ));
    }

    @Test
    void preventsDeletingCustomerWithAccounts()
            throws Exception {
        long customerId = createCustomer(
                "protected-owner@example.com",
                "Protected Owner"
        );

        long accountId = createAccount(
                customerId,
                "Protected Wallet"
        );

        mockMvc.perform(delete(
                        "/api/v1/customers/{id}",
                        customerId
                ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(
                        "$.code",
                        is("CUSTOMER_HAS_ACCOUNTS")
                ));

        mockMvc.perform(get(
                        "/api/v1/accounts/{id}",
                        accountId
                ))
                .andExpect(status().isOk());
    }

    private long createAccount(
            long customerId,
            String name
    ) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new CreateAccountRequest(customerId, name)
        );

        MvcResult result = mockMvc.perform(
                        post("/api/v1/accounts")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response.get("id").asLong();
    }

    private long createCustomer(
            String email,
            String fullName
    ) throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new CreateCustomerRequest(email, fullName)
        );

        MvcResult result = mockMvc.perform(
                        post("/api/v1/customers")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return response.get("id").asLong();
    }
}
