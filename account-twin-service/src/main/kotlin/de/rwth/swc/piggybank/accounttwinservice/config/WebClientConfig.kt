package de.rwth.swc.piggybank.accounttwinservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration for WebClient.
 */
@Configuration
class WebClientConfig {
    
    /**
     * Creates a WebClient.Builder bean.
     *
     * @return The WebClient.Builder
     */
    @Bean
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }
}