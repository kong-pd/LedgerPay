package com.ledgerpay.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI ledgerPayOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("LedgerPay API")
                        .version("v1")
                        .description(
                                "Customer and balance-free Account APIs"
                        )
        );
    }
}
