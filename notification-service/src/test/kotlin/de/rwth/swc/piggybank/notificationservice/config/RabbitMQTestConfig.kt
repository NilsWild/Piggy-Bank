package de.rwth.swc.piggybank.notificationservice.config

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container

/**
 * Configuration class for setting up RabbitMQ properties.
 * This class provides an ApplicationContextInitializer that sets the spring.rabbitmq.* properties
 * to point to the RabbitMQ container.
 * 
 * Usage:
 * ```
 * @ContextConfiguration(initializers = [RabbitMQConfig.Initializer::class])
 * ```
 */
open class RabbitMQTestConfig {
    companion object {
        private val log = LoggerFactory.getLogger(RabbitMQTestConfig::class.java)

        @Container
        val rabbitContainer: RabbitMQContainer = RabbitMQContainer("rabbitmq:3-management-alpine")
            .apply {
                start()
                try {
                    execInContainer("rabbitmqctl", "trace_on", "-p", "/")
                    log.info("RabbitMQ container started at {}:{}", host, amqpPort)
                    log.info("RabbitMQ HTTP URL: {}", httpUrl)
                } catch (e: Exception) {
                    log.error("Failed to enable RabbitMQ tracing", e)
                    throw RuntimeException("Failed to enable RabbitMQ tracing", e)
                }
            }

        /**
         * Gets the RabbitMQ properties from the RabbitMQ container.
         */
        fun getRabbitMQProperties(): Map<String, Any> {
            return mapOf(
                "spring.rabbitmq.host" to rabbitContainer.host,
                "spring.rabbitmq.port" to rabbitContainer.amqpPort,
                "spring.rabbitmq.username" to rabbitContainer.adminUsername,
                "spring.rabbitmq.password" to rabbitContainer.adminPassword,
                "spring.rabbitmq.httpUrl" to rabbitContainer.httpUrl
            )
        }
    }

    /**
     * ApplicationContextInitializer for setting up RabbitMQ properties.
     * This initializer sets the spring.rabbitmq.* properties to point to the RabbitMQ container.
     */
    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            val properties = getRabbitMQProperties()

            // Add the properties to the environment
            val propertySource = MapPropertySource("rabbitmq-properties", properties)
            applicationContext.environment.propertySources.addFirst(propertySource)
        }
    }
}