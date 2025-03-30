package de.rwth.swc.piggybank.transfergateway.config

import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

/**
 * Configuration class for setting up MockServer properties.
 * This class manages the MockServer container lifecycle and provides an ApplicationContextInitializer
 * that sets the account-twin-service.url property to point to the MockServer container.
 * 
 * Usage:
 * ```
 * @ContextConfiguration(initializers = [MockServerConfig.Initializer::class])
 * ```
 */
open class MockServerConfig {
    companion object {
        private val logger = LoggerFactory.getLogger(MockServerConfig::class.java)

        @Container
        val mockServerContainer: MockServerContainer = MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0")
        ).apply {
            withLogConsumer(Slf4jLogConsumer(logger))
            try {
                start()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

        /**
         * Gets the MockServer URL from the MockServer container.
         */
        fun getMockServerUrl(): String {
            return "http://${mockServerContainer.host}:${mockServerContainer.serverPort}"
        }

        /**
         * Creates a MockServerClient for the running MockServer container.
         * This can be used to set up expectations for the MockServer.
         */
        fun createMockServerClient(): MockServerClient {
            return MockServerClient(mockServerContainer.host, mockServerContainer.serverPort)
        }

        /**
         * Sets up an expectation for the MockServer to respond to a POST request to /api/transactions
         * with a 201 CREATED response.
         *
         * @param statusCode The HTTP status code to return (201 for created, 500 for error)
         */
        fun setupSendTransactionExpectation(statusCode: Int = 201) {
            try {
                createMockServerClient()
                    .`when`(
                        HttpRequest.request()
                            .withMethod("POST")
                            .withPath("/api/transactions")
                    )
                    .respond(
                        HttpResponse.response()
                            .withStatusCode(statusCode)
                            .withContentType(MediaType.APPLICATION_JSON)
                    )
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    /**
     * ApplicationContextInitializer for setting up MockServer properties.
     * This initializer sets the account-twin-service.url property to point to the MockServer container.
     */
    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(applicationContext: ConfigurableApplicationContext) {
            val mockServerUrl = getMockServerUrl()

            // Add the property to the environment
            val properties = mapOf("account-twin-service.url" to mockServerUrl)
            val propertySource = MapPropertySource("mockserver-properties", properties)
            applicationContext.environment.propertySources.addFirst(propertySource)

            // Also set the system property for backward compatibility
            System.setProperty("account-twin-service.url", mockServerUrl)
        }
    }
}