package com.ledgerpay;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class LedgerPayApplicationTests {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withUrlParam("sslMode", "DISABLED");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void openApiDescribesAllPhaseOneOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath(
                        "$.info.title",
                        is("LedgerPay API")
                ))
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/customers']['get']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/customers']['post']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/customers/{id}']['get']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/customers/{id}']['put']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/customers/{id}']['delete']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/accounts']['get']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/accounts']['post']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/accounts/{id}']['get']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/accounts/{id}']['put']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/accounts/{id}']['delete']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/customers']['post']" +
                                "['responses']['201']['content']" +
                                "['application/json']['schema']['$ref']",
                        is("#/components/schemas/CustomerResponse")
                ))
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/accounts']['post']" +
                                "['responses']['201']['content']" +
                                "['application/json']['schema']['$ref']",
                        is("#/components/schemas/AccountResponse")
                ))
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/customers/{id}']['delete']" +
                                "['responses']['204']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v1/accounts/{id}']['delete']" +
                                "['responses']['204']"
                ).exists());
    }

    @Test
    void swaggerUiEntryPointRedirectsToUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        endsWith("/swagger-ui/index.html")
                ));
    }
}
