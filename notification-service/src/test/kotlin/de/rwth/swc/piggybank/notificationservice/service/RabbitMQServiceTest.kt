package de.rwth.swc.piggybank.notificationservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountUpdatedEvent
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionAmountDto
import de.rwth.swc.piggybank.goalservice.dto.*
import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitMQServiceTest {

    private lateinit var rabbitTemplate: RabbitTemplate
    private lateinit var notificationService: NotificationService
    private lateinit var rabbitMQService: RabbitMQService

    private val accountId = "test-account-id"
    private val goalId = UUID.randomUUID()
    private val now = LocalDateTime.now()
    private val testInstant = Instant.parse("2023-01-01T12:00:00Z")

    @BeforeEach
    fun setup() {
        rabbitTemplate = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        val objectMapper = mockk<ObjectMapper>(relaxed = true)
        rabbitMQService = RabbitMQService(
            rabbitTemplate = rabbitTemplate,
            notificationService = notificationService,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `should handle goal updated event`() {
        // Given
        val event = GoalUpdatedEvent(
            eventType = "GOAL_UPDATED",
            goalId = goalId,
            goalName = "Test Goal",
            goalType = GoalType.SPENDING_LIMIT,
            goalStatus = GoalStatus.ACTIVE,
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
                goalType = GoalType.SPENDING_LIMIT,
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
            goalType = GoalType.SPENDING_LIMIT,
            goalStatus = GoalStatus.ACHIEVED,
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
            goalType = GoalType.SPENDING_LIMIT,
            goalStatus = GoalStatus.FAILED,
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
            goalType = GoalType.SPENDING_LIMIT,
            goalStatus = GoalStatus.ACTIVE,
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
                goalType = GoalType.SPENDING_LIMIT,
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
        val notification = Notification(
            accountId = accountId,
            eventType = NotificationEventType.GOAL_UPDATE,
            message = "Test message",
            createdAt = testInstant
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
        val notification = Notification(
            accountId = accountId,
            eventType = NotificationEventType.GOAL_UPDATE,
            message = "Test message",
            createdAt = testInstant
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
        val transactionId = UUID.randomUUID().toString()
        val transferId = UUID.randomUUID().toString()

        val eventDto = AccountUpdatedEvent(
            eventType = "ACCOUNT_UPDATED",
            accountId = accountId,
            accountType = "CHECKING",
            accountIdentifier = "DE123456789",
            value = "1000.00",
            currencyCode = "EUR",
            transactionId = transactionId,
            transferId = transferId,
            transactionAmount = TransactionAmountDto(
                value = "100.0",
                currencyCode = "EUR"
            ),
            transactionType = "DEPOSIT",
            transactionPurpose = "Test purpose"
        )

        // When
        rabbitMQService.handleAccountUpdatedEvent(eventDto)

        // Then
        verify {
            notificationService.processAccountUpdatedEvent(
                accountId = accountId,
                transactionType = "DEPOSIT",
                amount = 100.0,
                currencyCode = "EUR",
                purpose = "Test purpose"
            )
        }
    }

    @Test
    fun `should handle account updated event with String value`() {
        // Given
        val transactionId = UUID.randomUUID().toString()
        val transferId = UUID.randomUUID().toString()

        val eventDto = AccountUpdatedEvent(
            eventType = "ACCOUNT_UPDATED",
            accountId = accountId,
            accountType = "CHECKING",
            accountIdentifier = "DE123456789",
            value = "1000.00",
            currencyCode = "EUR",
            transactionId = transactionId,
            transferId = transferId,
            transactionAmount = TransactionAmountDto(
                value = "100.0",
                currencyCode = "EUR"
            ),
            transactionType = "DEPOSIT",
            transactionPurpose = "Test purpose"
        )

        // When
        rabbitMQService.handleAccountUpdatedEvent(eventDto)

        // Then
        verify {
            notificationService.processAccountUpdatedEvent(
                accountId = accountId,
                transactionType = "DEPOSIT",
                amount = 100.0,
                currencyCode = "EUR",
                purpose = "Test purpose"
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
