package de.rwth.swc.piggybank.accounttwinservice.util

import de.rwth.swc.piggybank.accounttwinservice.config.RabbitMQTestConfig
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Utility class for RabbitMQ testing.
 * This class provides static methods for waiting for RabbitMQ messages.
 */
object RabbitMQTestUtils {
    // Use the TEST_QUEUE_NAME constant from RabbitMQProperties
    private val TEST_QUEUE_NAME = RabbitMQTestConfig.TEST_QUEUE_NAME

    /**
     * Waits for a message to be received in the test queue and returns it.
     * 
     * @param rabbitTemplate The RabbitTemplate to use for receiving messages
     * @param timeout The maximum time to wait for the message
     * @param timeUnit The time unit of the timeout
     * @return The received message, or null if no message was received within the timeout
     */
    fun waitForMessage(
        rabbitTemplate: RabbitTemplate,
        timeout: Long = 5,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): Message? {
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
     * @param rabbitTemplate The RabbitTemplate to use for receiving messages
     * @param predicate A function that takes a Message and returns true if it matches the expected criteria
     * @param timeout The maximum time to wait for the message
     * @param timeUnit The time unit of the timeout
     */
    fun waitForMessageMatching(
        rabbitTemplate: RabbitTemplate,
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
     * @param rabbitTemplate The RabbitTemplate to use for receiving messages
     * @param eventType The expected event type (e.g., "ACCOUNT_CREATED")
     * @param timeout The maximum time to wait for the message
     * @param timeUnit The time unit of the timeout
     * @return The received message, or null if no matching message was received within the timeout
     */
    fun waitForEventType(
        rabbitTemplate: RabbitTemplate,
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
