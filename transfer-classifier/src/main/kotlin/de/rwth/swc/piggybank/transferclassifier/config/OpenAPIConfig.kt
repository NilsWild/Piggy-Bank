package de.rwth.swc.piggybank.transferclassifier.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for OpenAPI documentation.
 */
@Configuration
class OpenAPIConfig {

    /**
     * Configures the OpenAPI documentation.
     *
     * @return The OpenAPI configuration
     */
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Transfer Classifier API")
                    .description("API for classifying transfers in the Piggy Bank application")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Piggy Bank Team")
                            .email("piggybank@example.com")
                    )
            )
            .addServersItem(
                Server()
                    .url("/")
                    .description("Default Server URL")
            )
    }
}