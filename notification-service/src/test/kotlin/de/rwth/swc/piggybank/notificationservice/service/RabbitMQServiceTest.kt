package de.rwth.swc.piggybank.notificationservice.service

import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.dto.GoalAchievedEvent
import de.rwth.swc.piggybank.notificationservice.dto.GoalFailedEvent
import de.rwth.swc.piggybank.notificationservice.dto.GoalUpdatedEvent
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitMQServiceTest {

    private lateinit var rabbitTemplate: RabbitTemplate
    private lateinit var notificationService: NotificationService
    private lateinit var rabbitMQService: RabbitMQService

    private val accountId = "test-account-id"
    private val goalId = UUID.randomUUID()
    private val now = LocalDateTime.now()

    @BeforeEach
    fun setup() {
        rabbitTemplate = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        rabbitMQService = RabbitMQService(
            rabbitTemplate = rabbitTemplate,
            notificationService = notificationService
        )
    }

    @Test
    fun `should handle goal updated event`() {
        // Given
        val event = GoalUpdatedEvent(
            eventType = "GOAL_UPDATED",
            goalId = goalId,
            goalName = "Test Goal",
            goalType = "SPENDING_LIMIT",
            goalStatus = "ACTIVE",
            accountId = accountId,
            timestamp = now,
            progress = BigDecimal("50.00"),
            target = BigDecimal("100.00"),
            currencyCode = "EUR"
        )

        // When
        rabbitMQService.handleGoalUpdatedEvent(event)

        // Then
        verify {
            notificationService.processGoalUpdatedEvent(
                accountId = accountId,
                goalId = goalId,
                goalName = "Test Goal",
                goalType = "SPENDING_LIMIT",
                progress = BigDecimal("50.00"),
                target = BigDecimal("100.00"),
                currencyCode = "EUR"
            )
        }
    }

    @Test
    fun `should handle goal achieved event`() {
        // Given
        val event = GoalAchievedEvent(
            eventType = "GOAL_ACHIEVED",
            goalId = goalId,
            goalName = "Test Goal",
            goalType = "SPENDING_LIMIT",
            goalStatus = "ACHIEVED",
            accountId = accountId,
            timestamp = now
        )

        // When
        rabbitMQService.handleGoalAchievedEvent(event)

        // Then
        verify {
            notificationService.processGoalAchievedEvent(
                accountId = accountId,
                goalId = goalId,
                goalName = "Test Goal"
            )
        }
    }

    @Test
    fun `should handle goal failed event`() {
        // Given
        val event = GoalFailedEvent(
            eventType = "GOAL_FAILED",
            goalId = goalId,
            goalName = "Test Goal",
            goalType = "SPENDING_LIMIT",
            goalStatus = "FAILED",
            accountId = accountId,
            timestamp = now
        )

        // When
        rabbitMQService.handleGoalFailedEvent(event)

        // Then
        verify {
            notificationService.processGoalFailedEvent(
                accountId = accountId,
                goalId = goalId,
                goalName = "Test Goal"
            )
        }
    }

    @Test
    fun `should handle exception in goal updated event processing`() {
        // Given
        val event = GoalUpdatedEvent(
            eventType = "GOAL_UPDATED",
            goalId = goalId,
            goalName = "Test Goal",
            goalType = "SPENDING_LIMIT",
            goalStatus = "ACTIVE",
            accountId = accountId,
            timestamp = now,
            progress = BigDecimal("50.00"),
            target = BigDecimal("100.00"),
            currencyCode = "EUR"
        )

        every {
            notificationService.processGoalUpdatedEvent(
                any(), any(), any(), any(), any(), any(), any()
            )
        } throws RuntimeException("Test exception")

        // When
        rabbitMQService.handleGoalUpdatedEvent(event)

        // Then
        verify {
            notificationService.processGoalUpdatedEvent(
                accountId = accountId,
                goalId = goalId,
                goalName = "Test Goal",
                goalType = "SPENDING_LIMIT",
                progress = BigDecimal("50.00"),
                target = BigDecimal("100.00"),
                currencyCode = "EUR"
            )
        }
        // No exception should be thrown outside the handler
    }

    @Test
    fun `should send notification to RabbitMQ`() {
        // Given
        val notification = Notification.create(
            accountId = accountId,
            eventType = NotificationEventType.GOAL_UPDATE,
            message = "Test message"
        )

        // When
        rabbitMQService.sendNotification(notification)

        // Then
        verify {
            rabbitTemplate.convertAndSend(
                RabbitMQService.NOTIFICATION_EXCHANGE_NAME,
                RabbitMQService.NOTIFICATION_ROUTING_KEY,
                match<Map<String, Any>> {
                    it["id"] == notification.id.toString() &&
                    it["accountId"] == notification.accountId &&
                    it["eventType"] == notification.eventType.name &&
                    it["message"] == notification.message &&
                    it["read"] == notification.read &&
                    it["createdAt"] == notification.createdAt.toString()
                }
            )
        }
    }

    @Test
    fun `should handle exception when sending notification`() {
        // Given
        val notification = Notification.create(
            accountId = accountId,
            eventType = NotificationEventType.GOAL_UPDATE,
            message = "Test message"
        )

        every {
            rabbitTemplate.convertAndSend(any(), any(), any<Map<String, Any>>())
        } throws RuntimeException("Test exception")

        // When/Then
        assertThrows<RuntimeException> {
            rabbitMQService.sendNotification(notification)
        }
    }

    @Test
    fun `should handle account updated event with Number value`() {
        // Given
        val event = mapOf(
            "eventType" to "ACCOUNT_UPDATED",
            "accountId" to accountId,
            "transactionType" to "DEPOSIT",
            "transactionAmount" to mapOf(
                "value" to 100.0,
                "currencyCode" to "EUR"
            ),
            "transactionPurpose" to "Test purpose",
            "sourceAccount" to "source-account",
            "destinationAccount" to "destination-account"
        )

        // When
        rabbitMQService.handleAccountUpdatedEvent(event)

        // Then
        verify {
            notificationService.processAccountUpdatedEvent(
                accountId = accountId,
                transactionType = "DEPOSIT",
                amount = 100.0,
                currencyCode = "EUR",
                purpose = "Test purpose",
                sourceAccount = "source-account",
                destinationAccount = "destination-account"
            )
        }
    }

    @Test
    fun `should handle account updated event with String value`() {
        // Given
        val event = mapOf(
            "eventType" to "ACCOUNT_UPDATED",
            "accountId" to accountId,
            "transactionType" to "DEPOSIT",
            "transactionAmount" to mapOf(
                "value" to "100.0",
                "currencyCode" to "EUR"
            ),
            "transactionPurpose" to "Test purpose",
            "sourceAccount" to "source-account",
            "destinationAccount" to "destination-account"
        )

        // When
        rabbitMQService.handleAccountUpdatedEvent(event)

        // Then
        verify {
            notificationService.processAccountUpdatedEvent(
                accountId = accountId,
                transactionType = "DEPOSIT",
                amount = 100.0,
                currencyCode = "EUR",
                purpose = "Test purpose",
                sourceAccount = "source-account",
                destinationAccount = "destination-account"
            )
        }
    }

    private inline fun <reified T : Throwable> assertThrows(crossinline block: () -> Unit): T {
        try {
            block()
            throw AssertionError("Expected ${T::class.java.simpleName} to be thrown, but nothing was thrown")
        } catch (e: Throwable) {
            if (e is T) {
                return e
            }
            throw AssertionError("Expected ${T::class.java.simpleName} to be thrown, but ${e::class.java.simpleName} was thrown", e)
        }
    }
}
