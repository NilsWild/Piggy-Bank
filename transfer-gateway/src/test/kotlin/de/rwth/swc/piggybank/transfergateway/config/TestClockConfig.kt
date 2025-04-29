package de.rwth.swc.piggybank.transfergateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Configuration for providing a fixed Clock bean for testing.
 */
@Configuration
class TestClockConfig {
    
    /**
     * Creates a fixed Clock bean for testing.
     * 
     * @return A fixed Clock set to 2023-01-01T12:00:00Z
     */
    @Bean
    @Primary
    fun fixedClock(): Clock {
        // Fixed time for testing: 2023-01-01T12:00:00Z
        return Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC"))
    }
}