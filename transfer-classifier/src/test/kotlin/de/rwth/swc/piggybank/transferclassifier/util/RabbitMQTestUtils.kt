package de.rwth.swc.piggybank.transferclassifier.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionTimeoutException
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Utility class for testing RabbitMQ.
 */
object RabbitMQTestUtils {
    private val log = LoggerFactory.getLogger(RabbitMQTestUtils::class.java)

    /**
     * Waits for a message to arrive in the specified queue.
     *
     * @param rabbitTemplate The RabbitTemplate to use for receiving messages
     * @param queueName The name of the queue to receive from
     * @param timeout The maximum time to wait
     * @param unit The time unit of the timeout
     * @return The message, or null if no message was received within the timeout
     */
    fun waitForMessage(
        rabbitTemplate: RabbitTemplate,
        queueName: String,
        timeout: Long,
        unit: TimeUnit
    ): Message? {
        val messageRef = AtomicReference<Message>()
        try {
            await().atMost(timeout, unit)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until {
                    val message = rabbitTemplate.receive(queueName)
                    if (message != null) {
                        log.info("Received message from queue {}: {}", queueName, String(message.body))
                        messageRef.set(message)
                        true
                    } else {
                        false
                    }
                }
            return messageRef.get()
        } catch (e: ConditionTimeoutException) {
            log.warn("No message received from queue {} within {} {}", queueName, timeout, unit)
            return null
        }
    }

    /**
     * Waits for a message with the specified event type to arrive in the specified queue.
     *
     * @param rabbitTemplate The RabbitTemplate to use for receiving messages
     * @param eventType The event type to wait for
     * @param timeout The maximum time to wait
     * @param unit The time unit of the timeout
     * @return The message, or null if no message with the specified event type was received within the timeout
     */
    fun waitForEventType(
        rabbitTemplate: RabbitTemplate,
        eventType: String,
        timeout: Long,
        unit: TimeUnit
    ): Message? {
        val messageRef = AtomicReference<Message>()
        try {
            await().atMost(timeout, unit)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until {
                    val message = rabbitTemplate.receive("test_queue")
                    if (message != null) {
                        val messageBody = String(message.body)
                        log.info("Received message: {}", messageBody)
                        if (messageBody.contains("\"eventType\":\"$eventType\"")) {
                            messageRef.set(message)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            return messageRef.get()
        } catch (e: ConditionTimeoutException) {
            log.warn("No message with event type {} received within {} {}", eventType, timeout, unit)
            return null
        }
    }

    /**
     * Waits for a message matching the specified predicate to arrive in the specified queue.
     *
     * @param rabbitTemplate The RabbitTemplate to use for receiving messages
     * @param queueName The name of the queue to receive from
     * @param predicate The predicate to match against the message body
     * @param timeout The maximum time to wait
     * @param unit The time unit of the timeout
     * @return The message, or null if no matching message was received within the timeout
     */
    fun waitForMessageMatching(
        rabbitTemplate: RabbitTemplate,
        queueName: String,
        predicate: (String) -> Boolean,
        timeout: Long,
        unit: TimeUnit
    ): Message? {
        val messageRef = AtomicReference<Message>()
        try {
            await().atMost(timeout, unit)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until {
                    val message = rabbitTemplate.receive(queueName)
                    if (message != null) {
                        val messageBody = String(message.body)
                        log.info("Received message from queue {}: {}", queueName, messageBody)
                        if (predicate(messageBody)) {
                            messageRef.set(message)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            return messageRef.get()
        } catch (e: ConditionTimeoutException) {
            log.warn("No matching message received from queue {} within {} {}", queueName, timeout, unit)
            return null
        }
    }
}
