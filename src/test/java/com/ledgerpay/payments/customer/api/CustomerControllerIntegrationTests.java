package com.ledgerpay.payments.customer.api;

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
class CustomerControllerIntegrationTests {

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
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
    }

    @Test
    void createsCustomerWithNormalizedEmail() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Aisha.Example@Example.COM",
                                  "fullName": "Aisha Example"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        startsWith("/api/v1/customers/")
                ))
                .andExpect(jsonPath(
                        "$.email",
                        is("aisha.example@example.com")
                ))
                .andExpect(jsonPath(
                        "$.fullName",
                        is("Aisha Example")
                ))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void rejectsInvalidEmailWithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "fullName": "Aisha Example"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        is("VALIDATION_FAILED")
                ))
                .andExpect(jsonPath(
                        "$.fieldErrors",
                        hasSize(1)
                ))
                .andExpect(jsonPath(
                        "$.fieldErrors[0].field",
                        is("email")
                ));
    }

    @Test
    void rejectsDuplicateNormalizedEmail() throws Exception {
        createCustomer(
                "duplicate@example.com",
                "First Customer"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "DUPLICATE@EXAMPLE.COM",
                                  "fullName": "Second Customer"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(
                        "$.code",
                        is("CUSTOMER_EMAIL_CONFLICT")
                ))
                .andExpect(jsonPath(
                        "$.fieldErrors",
                        hasSize(0)
                ));
    }

    @Test
    void returnsNotFoundForMissingCustomer() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/customers/{id}",
                        999_999
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(
                        "$.code",
                        is("CUSTOMER_NOT_FOUND")
                ));
    }

    @Test
    void rejectsUnknownStatusWithGlobalErrorFormat()
            throws Exception {
        long id = createCustomer(
                "status@example.com",
                "Status Customer"
        );

        mockMvc.perform(put(
                        "/api/v1/customers/{id}",
                        id
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "status@example.com",
                                  "fullName": "Status Customer",
                                  "status": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.code",
                        is("MALFORMED_REQUEST")
                ))
                .andExpect(jsonPath(
                        "$.fieldErrors",
                        hasSize(0)
                ));
    }

    @Test
    void paginatesAndFiltersCustomersByStatus()
            throws Exception {
        createCustomer(
                "active@example.com",
                "Active Customer"
        );

        long suspendedId = createCustomer(
                "suspended@example.com",
                "Suspended Customer"
        );

        mockMvc.perform(put(
                        "/api/v1/customers/{id}",
                        suspendedId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "suspended@example.com",
                                  "fullName": "Suspended Customer",
                                  "status": "SUSPENDED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customers")
                        .param("page", "0")
                        .param("size", "1")
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content",
                        hasSize(1)
                ))
                .andExpect(jsonPath(
                        "$.content[0].email",
                        is("suspended@example.com")
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
    void updatesAndDeletesCustomer() throws Exception {
        long id = createCustomer(
                "before@example.com",
                "Before Name"
        );

        mockMvc.perform(put(
                        "/api/v1/customers/{id}",
                        id
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "after@example.com",
                                  "fullName": "After Name",
                                  "status": "SUSPENDED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.email",
                        is("after@example.com")
                ))
                .andExpect(jsonPath(
                        "$.fullName",
                        is("After Name")
                ))
                .andExpect(jsonPath(
                        "$.status",
                        is("SUSPENDED")
                ));

        mockMvc.perform(delete(
                        "/api/v1/customers/{id}",
                        id
                ))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                        "/api/v1/customers/{id}",
                        id
                ))
                .andExpect(status().isNotFound());
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
