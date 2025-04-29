package de.rwth.swc.piggybank.transfergateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Configuration for time-related beans.
 */
@Configuration
class TimeConfig {

    /**
     * Creates a Clock bean that can be used for getting the current time.
     * This allows for easier testing by making the time controllable.
     *
     * @return The Clock instance
     */
    @Bean
    fun clock(): Clock {
        return Clock.systemDefaultZone()
    }
}