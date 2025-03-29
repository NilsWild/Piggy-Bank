package de.rwth.swc.piggybank.transfergateway

import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Import(SpringAMQPInterACtObserverConfiguration::class, AmqpTestConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
abstract class AmqpBaseTest {

    private val log = LoggerFactory.getLogger(AmqpBaseTest::class.java)

    @Autowired
    protected lateinit var rabbitTemplate: RabbitTemplate

    companion object {
        const val TEST_QUEUE_NAME = "test_queue"

        @Container
        val rabbitContainer: RabbitMQContainer = RabbitMQContainer("rabbitmq:3-management-alpine")
            .apply {
                start()
                try {
                    execInContainer("rabbitmqctl", "trace_on", "-p", "/")
                } catch (e: Exception) {
                    throw RuntimeException("Failed to enable RabbitMQ tracing", e)
                }
            }

        @JvmStatic
        @DynamicPropertySource
        fun registerRabbitMQProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.rabbitmq.host") { rabbitContainer.host }
            registry.add("spring.rabbitmq.port") { rabbitContainer.amqpPort }
            registry.add("spring.rabbitmq.username") { rabbitContainer.adminUsername }
            registry.add("spring.rabbitmq.password") { rabbitContainer.adminPassword }
            registry.add("spring.rabbitmq.httpUrl") { rabbitContainer.httpUrl }
        }
    }

    init {
        // Log connection details for debugging
        log.info("RabbitMQ container started at {}:{}", rabbitContainer.host, rabbitContainer.amqpPort)
        log.info("RabbitMQ HTTP URL: {}", rabbitContainer.httpUrl)
    }

    /**
     * Waits for a message to be received in the test queue and returns it.
     * 
     * @param timeout The maximum time to wait for the message
     * @param timeUnit The time unit of the timeout
     * @return The received message, or null if no message was received within the timeout
     */
    protected fun waitForMessage(timeout: Long = 5, timeUnit: TimeUnit = TimeUnit.SECONDS): Message? {
        val messageRef = AtomicReference<Message>()

        await.atMost(timeout, timeUnit).untilNotNull {
            val message = rabbitTemplate.receive(TEST_QUEUE_NAME)
            messageRef.set(message)
            message
        }

        return messageRef.get()
    }

    /**
     * Waits for a message to be received in the test queue and verifies it using the provided predicate.
     * 
     * @param predicate A function that takes a Message and returns true if it matches the expected criteria
     * @param timeout The maximum time to wait for the message
     * @param timeUnit The time unit of the timeout
     */
    protected fun waitForMessageMatching(
        predicate: (Message) -> Boolean,
        timeout: Long = 5,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ) {
        Awaitility.await()
            .atMost(timeout, timeUnit)
            .pollInterval(Duration.ofMillis(100))
            .until {
                val message = rabbitTemplate.receive(TEST_QUEUE_NAME)
                message != null && predicate(message)
            }
    }

    /**
     * Waits for a message with the specified event type to be received in the test queue.
     * 
     * @param eventType The expected event type (e.g., "ACCOUNT_CREATED")
     * @param timeout The maximum time to wait for the message
     * @param timeUnit The time unit of the timeout
     * @return The received message, or null if no matching message was received within the timeout
     */
    protected fun waitForEventType(
        eventType: String,
        timeout: Long = 5,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): Message? {
        val messageRef = AtomicReference<Message>()

        Awaitility.await()
            .atMost(timeout, timeUnit)
            .pollInterval(Duration.ofMillis(100))
            .until {
                val message = rabbitTemplate.receive(TEST_QUEUE_NAME)
                if (message != null) {
                    val messageBody = String(message.body)
                    val containsEventType = messageBody.contains("\"eventType\":\"$eventType\"")
                    if (containsEventType) {
                        messageRef.set(message)
                        return@until true
                    }
                }
                false
            }

        return messageRef.get()
    }
}