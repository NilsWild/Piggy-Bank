package de.rwth.swc.piggybank.notificationservice.service

import de.rwth.swc.piggybank.goalservice.dto.GoalType
import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.domain.NotificationSubscription
import de.rwth.swc.piggybank.notificationservice.repository.NotificationRepository
import de.rwth.swc.piggybank.notificationservice.repository.NotificationSubscriptionRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationServiceTest {

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var subscriptionRepository: NotificationSubscriptionRepository
    private lateinit var rabbitMQService: RabbitMQService
    private lateinit var clock: Clock
    private lateinit var notificationService: NotificationService

    private val accountId = "test-account-id"
    private val goalId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val testInstant = Instant.parse("2023-01-01T12:00:00Z")

    @BeforeEach
    fun setup() {
        notificationRepository = mockk(relaxed = true)
        subscriptionRepository = mockk(relaxed = true)
        rabbitMQService = mockk(relaxed = true)
        clock = Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC"))
        notificationService = NotificationService(
            notificationRepository = notificationRepository,
            subscriptionRepository = subscriptionRepository,
            rabbitMQService = rabbitMQService,
            clock = clock
        )
    }

    @Test
    fun `should process goal updated event with active subscriptions`() {
        // Given
        val subscriptions = listOf(
            NotificationSubscription(
                accountId = accountId,
                eventType = NotificationEventType.GOAL_UPDATE,
                createdAt = testInstant
            )
        )
        every {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_UPDATE
            )
        } returns subscriptions

        val savedNotification = slot<Notification>()
        every { notificationRepository.save(capture(savedNotification)) } answers { savedNotification.captured }

        // When
        notificationService.processGoalUpdatedEvent(
            accountId = accountId,
            goalId = goalId,
            goalName = "Test Goal",
            goalType = GoalType.SPENDING_LIMIT,
            progress = BigDecimal("50.00"),
            target = BigDecimal("100.00"),
            currencyCode = "EUR"
        )

        // Then
        verify {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_UPDATE
            )
        }
        verify { notificationRepository.save(any()) }
        verify { rabbitMQService.sendNotification(savedNotification.captured) }

        // Verify notification properties
        assert(savedNotification.captured.accountId == accountId)
        assert(savedNotification.captured.eventType == NotificationEventType.GOAL_UPDATE)
        assert(savedNotification.captured.message.contains("Test Goal"))
        assert(savedNotification.captured.message.contains("50%"))
        assert(savedNotification.captured.message.contains("50.00"))
        assert(savedNotification.captured.message.contains("100.00"))
        assert(savedNotification.captured.message.contains("EUR"))
    }

    @Test
    fun `should not process goal updated event without active subscriptions`() {
        // Given
        every {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_UPDATE
            )
        } returns emptyList()

        // When
        notificationService.processGoalUpdatedEvent(
            accountId = accountId,
            goalId = goalId,
            goalName = "Test Goal",
            goalType = GoalType.SPENDING_LIMIT,
            progress = BigDecimal("50.00"),
            target = BigDecimal("100.00"),
            currencyCode = "EUR"
        )

        // Then
        verify {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_UPDATE
            )
        }
        verify(exactly = 0) { notificationRepository.save(any()) }
        verify(exactly = 0) { rabbitMQService.sendNotification(any()) }
    }

    @Test
    fun `should process goal achieved event with active subscriptions`() {
        // Given
        val subscriptions = listOf(
            NotificationSubscription(
                accountId = accountId,
                eventType = NotificationEventType.GOAL_ACHIEVED,
                createdAt = testInstant
            )
        )
        every {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_ACHIEVED
            )
        } returns subscriptions

        val savedNotification = slot<Notification>()
        every { notificationRepository.save(capture(savedNotification)) } answers { savedNotification.captured }

        // When
        notificationService.processGoalAchievedEvent(
            accountId = accountId,
            goalId = goalId,
            goalName = "Test Goal"
        )

        // Then
        verify {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_ACHIEVED
            )
        }
        verify { notificationRepository.save(any()) }
        verify { rabbitMQService.sendNotification(savedNotification.captured) }

        // Verify notification properties
        assert(savedNotification.captured.accountId == accountId)
        assert(savedNotification.captured.eventType == NotificationEventType.GOAL_ACHIEVED)
        assert(savedNotification.captured.message.contains("Test Goal"))
        assert(savedNotification.captured.message.contains("achieved"))
    }

    @Test
    fun `should not process goal achieved event without active subscriptions`() {
        // Given
        every {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_ACHIEVED
            )
        } returns emptyList()

        // When
        notificationService.processGoalAchievedEvent(
            accountId = accountId,
            goalId = goalId,
            goalName = "Test Goal"
        )

        // Then
        verify {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_ACHIEVED
            )
        }
        verify(exactly = 0) { notificationRepository.save(any()) }
        verify(exactly = 0) { rabbitMQService.sendNotification(any()) }
    }

    @Test
    fun `should process goal failed event with active subscriptions`() {
        // Given
        val subscriptions = listOf(
            NotificationSubscription(
                accountId = accountId,
                eventType = NotificationEventType.GOAL_FAILED,
                createdAt = testInstant
            )
        )
        every {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_FAILED
            )
        } returns subscriptions

        val savedNotification = slot<Notification>()
        every { notificationRepository.save(capture(savedNotification)) } answers { savedNotification.captured }

        // When
        notificationService.processGoalFailedEvent(
            accountId = accountId,
            goalId = goalId,
            goalName = "Test Goal"
        )

        // Then
        verify {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_FAILED
            )
        }
        verify { notificationRepository.save(any()) }
        verify { rabbitMQService.sendNotification(savedNotification.captured) }

        // Verify notification properties
        assert(savedNotification.captured.accountId == accountId)
        assert(savedNotification.captured.eventType == NotificationEventType.GOAL_FAILED)
        assert(savedNotification.captured.message.contains("Test Goal"))
        assert(savedNotification.captured.message.contains("failed"))
    }

    @Test
    fun `should not process goal failed event without active subscriptions`() {
        // Given
        every {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_FAILED
            )
        } returns emptyList()

        // When
        notificationService.processGoalFailedEvent(
            accountId = accountId,
            goalId = goalId,
            goalName = "Test Goal"
        )

        // Then
        verify {
            subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
                accountId,
                NotificationEventType.GOAL_FAILED
            )
        }
        verify(exactly = 0) { notificationRepository.save(any()) }
        verify(exactly = 0) { rabbitMQService.sendNotification(any()) }
    }
}
