package de.rwth.swc.piggybank.notificationservice.service

import de.interact.amqp.TestAmqpClient
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.amqp.AmqpMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountUpdatedEvent
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionAmountDto
import de.rwth.swc.piggybank.goalservice.dto.*
import de.rwth.swc.piggybank.notificationservice.AmqpTestConfig
import de.rwth.swc.piggybank.notificationservice.NotificationServiceApplication
import de.rwth.swc.piggybank.notificationservice.config.InterACtConfig
import de.rwth.swc.piggybank.notificationservice.config.RabbitMQTestConfig
import de.rwth.swc.piggybank.notificationservice.config.TestClockConfig
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.domain.NotificationSubscription
import de.rwth.swc.piggybank.notificationservice.repository.NotificationRepository
import de.rwth.swc.piggybank.notificationservice.repository.NotificationSubscriptionRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@Disabled
@SpringBootTest(
    classes = [NotificationServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class, TestClockConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [RabbitMQTestConfig.Initializer::class])
class RabbitMQServiceInterACtTest {

    companion object {
        // Fixed UUIDs for tests
        val TRANSACTION_ID_1 = UUID.fromString("7a2c259a-f63a-4951-a876-a2e8a7d1399b")
        val TRANSFER_ID_1 = UUID.fromString("df522f72-a23c-439b-bf8d-2cc0e7257551")
        val TRANSACTION_ID_2 = UUID.fromString("8f7e6d5c-4b3a-2a1b-0c9d-8e7f6a5b4c3d")
        val TRANSFER_ID_2 = UUID.fromString("9f8e7d6c-5b4a-3a2b-1c0d-9e8f7a6b5c4e")
        val GOAL_ID_1 = UUID.fromString("1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d")
        val GOAL_ID_2 = UUID.fromString("2b3c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e")

        // Constants for account exchange and routing keys
        const val ACCOUNT_EXCHANGE_NAME = "piggybank.accounts"
        const val ACCOUNT_UPDATED_ROUTING_KEY = "account.updated"

        // Constants for goal exchange and routing keys
        const val GOAL_EXCHANGE_NAME = "piggybank.goals"
        const val GOAL_UPDATED_ROUTING_KEY = "goal.updated"
        const val GOAL_ACHIEVED_ROUTING_KEY = "goal.achieved"
        const val GOAL_FAILED_ROUTING_KEY = "goal.failed"
    }

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var testAmqpClient: TestAmqpClient

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var subscriptionRepository: NotificationSubscriptionRepository

    @Autowired
    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
        subscriptionRepository.deleteAll()

        // Clear the RabbitMQ queue before each test
        rabbitTemplate.execute<Nothing> { channel ->
            channel.queuePurge("test_queue")
            null
        }
    }

    @InterACtTest
    @MethodSource("accountUpdatedEvents")
    fun `should process account update event and send notification`(eventStimulus: AmqpMessage<AccountUpdatedEvent>) {
        // Given
        val accountId = eventStimulus.body.accountId

        // Create a subscription for the account
        createTestSubscription(accountId, NotificationEventType.BALANCE_UPDATE)

        // When - Send the account update event
        testAmqpClient.send(
            ACCOUNT_EXCHANGE_NAME,
            ACCOUNT_UPDATED_ROUTING_KEY,
            eventStimulus
        )

        // Then - Wait for the notification to be sent
        await().atMost(5, TimeUnit.SECONDS).until {
            // Check if a notification was created in the repository
            val pageable = PageRequest.of(0, 10)
            val notifications = notificationRepository.findByAccountId(accountId, pageable)
            !notifications.isEmpty()
        }

        // And - Verify that a notification was sent to RabbitMQ
        val message = waitForMessage(5)
        message shouldNotBe null

        // Convert the message body to a string and verify it contains the expected data
        val messageBody = String(message!!.body)
        messageBody shouldContain "\"eventType\":\"BALANCE_UPDATE\""
        messageBody shouldContain "\"accountId\":\"$accountId\""
    }

    @Disabled
    @InterACtTest
    @MethodSource("accountUpdatedEventsNoSubscription")
    fun `should not send notification when no subscription exists`(eventStimulus: AmqpMessage<AccountUpdatedEvent>) {
        // Given
        val accountId = eventStimulus.body.accountId

        // When - Send the account update event
        testAmqpClient.send(
            ACCOUNT_EXCHANGE_NAME,
            ACCOUNT_UPDATED_ROUTING_KEY,
            eventStimulus
        )

        // Then - Wait a bit to ensure no notification is sent
        TimeUnit.SECONDS.sleep(2)

        // And - Verify that no notification was created in the repository
        val pageable = PageRequest.of(0, 10)
        val notifications = notificationRepository.findByAccountId(accountId, pageable)
        notifications.isEmpty() shouldBe true

        // And - Verify that no notification was sent to RabbitMQ
        val message = waitForMessage(1)
        message shouldBe null
    }

    fun accountUpdatedEvents(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to ACCOUNT_EXCHANGE_NAME,
                        "routingKey" to ACCOUNT_UPDATED_ROUTING_KEY
                    ),
                    AccountUpdatedEvent(
                        eventType = "ACCOUNT_UPDATED",
                        accountId = "test-account-456",
                        accountType = "CHECKING",
                        accountIdentifier = "DE987654321",
                        value = "900.00",
                        currencyCode = "EUR",
                        transactionId = TRANSACTION_ID_1.toString(),
                        transferId = TRANSFER_ID_1.toString(),
                        transactionAmount = TransactionAmountDto(
                            value = "50.00",
                            currencyCode = "EUR"
                        ),
                        transactionType = "DEBIT",
                        transactionPurpose = "Grocery shopping",
                    )
                )
            )
        )
    }

    fun accountUpdatedEventsNoSubscription(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to ACCOUNT_EXCHANGE_NAME,
                        "routingKey" to ACCOUNT_UPDATED_ROUTING_KEY
                    ),
                    AccountUpdatedEvent(
                        eventType = "ACCOUNT_UPDATED",
                        accountId = "test-account-no-subscription",
                        accountType = "CHECKING",
                        accountIdentifier = "DE123456789",
                        value = "1100.00",
                        currencyCode = "EUR",
                        transactionId = TRANSACTION_ID_2.toString(),
                        transferId = TRANSFER_ID_2.toString(),
                        transactionAmount = TransactionAmountDto(
                            value = "100.00",
                            currencyCode = "EUR"
                        ),
                        transactionType = "CREDIT",
                        transactionPurpose = "Salary payment"
                    )
                )
            )
        )
    }

    fun goalUpdatedEvents(): Stream<Arguments> {
        val now = LocalDateTime.now(clock)
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to GOAL_EXCHANGE_NAME,
                        "routingKey" to GOAL_UPDATED_ROUTING_KEY
                    ),
                    GoalUpdatedEvent(
                        eventType = "GOAL_UPDATED",
                        goalId = GOAL_ID_1,
                        goalName = "Vacation Savings",
                        goalType = GoalType.SAVINGS,
                        goalStatus = GoalStatus.ACTIVE,
                        accountId = "test-account-789",
                        timestamp = now,
                        progress = BigDecimal("500.00"),
                        target = BigDecimal("1000.00"),
                        currencyCode = "EUR"
                    )
                )
            )
        )
    }

    fun goalUpdatedEventsNoSubscription(): Stream<Arguments> {
        val now = LocalDateTime.now(clock)
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to GOAL_EXCHANGE_NAME,
                        "routingKey" to GOAL_UPDATED_ROUTING_KEY
                    ),
                    GoalUpdatedEvent(
                        eventType = "GOAL_UPDATED",
                        goalId = GOAL_ID_2,
                        goalName = "Budget Goal",
                        goalType = GoalType.SPENDING_LIMIT,
                        goalStatus = GoalStatus.ACTIVE,
                        accountId = "test-account-no-subscription-goal",
                        timestamp = now,
                        progress = BigDecimal("300.00"),
                        target = BigDecimal("500.00"),
                        currencyCode = "EUR"
                    )
                )
            )
        )
    }

    fun goalAchievedEvents(): Stream<Arguments> {
        val now = LocalDateTime.now(clock)
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to GOAL_EXCHANGE_NAME,
                        "routingKey" to GOAL_ACHIEVED_ROUTING_KEY
                    ),
                    GoalAchievedEvent(
                        eventType = "GOAL_ACHIEVED",
                        goalId = GOAL_ID_1,
                        goalName = "Vacation Savings",
                        goalType = GoalType.SAVINGS,
                        goalStatus = GoalStatus.ACHIEVED,
                        accountId = "test-account-456",
                        timestamp = now
                    )
                )
            )
        )
    }

    fun goalFailedEvents(): Stream<Arguments> {
        val now = LocalDateTime.now(clock)
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to GOAL_EXCHANGE_NAME,
                        "routingKey" to GOAL_FAILED_ROUTING_KEY
                    ),
                    GoalFailedEvent(
                        eventType = "GOAL_FAILED",
                        goalId = GOAL_ID_2,
                        goalName = "Budget Goal",
                        goalType = GoalType.SPENDING_LIMIT,
                        goalStatus = GoalStatus.FAILED,
                        accountId = "test-account-123",
                        timestamp = now
                    )
                )
            )
        )
    }

    @InterACtTest
    @MethodSource("goalUpdatedEvents")
    fun `should process goal update event and send notification`(eventStimulus: AmqpMessage<GoalUpdatedEvent>) {
        // Given
        val accountId = eventStimulus.body.accountId
        val goalName = eventStimulus.body.goalName

        // Create a subscription for the account
        createTestSubscription(accountId, NotificationEventType.GOAL_UPDATE)

        // When - Send the goal update event
        testAmqpClient.send(
            GOAL_EXCHANGE_NAME,
            GOAL_UPDATED_ROUTING_KEY,
            eventStimulus
        )

        // Then - Wait for the notification to be sent
        await().atMost(5, TimeUnit.SECONDS).until {
            // Check if a notification was created in the repository
            val pageable = PageRequest.of(0, 10)
            val notifications = notificationRepository.findByAccountId(accountId, pageable)
            !notifications.isEmpty()
        }

        // And - Verify that a notification was sent to RabbitMQ
        val message = waitForMessage(5)
        message shouldNotBe null

        // Convert the message body to a string and verify it contains the expected data
        val messageBody = String(message!!.body)
        messageBody shouldContain "\"eventType\":\"GOAL_UPDATE\""
        messageBody shouldContain "\"accountId\":\"$accountId\""
        messageBody shouldContain goalName
    }

    @InterACtTest
    @MethodSource("goalUpdatedEventsNoSubscription")
    fun `should not send goal update notification when no subscription exists`(eventStimulus: AmqpMessage<GoalUpdatedEvent>) {
        // Given
        val accountId = eventStimulus.body.accountId

        // When - Send the goal update event
        testAmqpClient.send(
            GOAL_EXCHANGE_NAME,
            GOAL_UPDATED_ROUTING_KEY,
            eventStimulus
        )

        // Then - Wait a bit to ensure no notification is sent
        TimeUnit.SECONDS.sleep(2)

        // And - Verify that no notification was created in the repository
        val pageable = PageRequest.of(0, 10)
        val notifications = notificationRepository.findByAccountId(accountId, pageable)
        notifications.isEmpty() shouldBe true

        // And - Verify that no notification was sent to RabbitMQ
        val message = waitForMessage(1)
        message shouldBe null
    }

    @InterACtTest
    @MethodSource("goalAchievedEvents")
    fun `should process goal achieved event and send notification`(eventStimulus: AmqpMessage<GoalAchievedEvent>) {
        // Given
        val accountId = eventStimulus.body.accountId
        val goalName = eventStimulus.body.goalName

        // Create a subscription for the account
        createTestSubscription(accountId, NotificationEventType.GOAL_ACHIEVED)

        // When - Send the goal achieved event
        testAmqpClient.send(
            GOAL_EXCHANGE_NAME,
            GOAL_ACHIEVED_ROUTING_KEY,
            eventStimulus
        )

        // Then - Wait for the notification to be sent
        await().atMost(5, TimeUnit.SECONDS).until {
            // Check if a notification was created in the repository
            val pageable = PageRequest.of(0, 10)
            val notifications = notificationRepository.findByAccountId(accountId, pageable)
            !notifications.isEmpty()
        }

        // And - Verify that a notification was sent to RabbitMQ
        val message = waitForMessage(5)
        message shouldNotBe null

        // Convert the message body to a string and verify it contains the expected data
        val messageBody = String(message!!.body)
        messageBody shouldContain "\"eventType\":\"GOAL_ACHIEVED\""
        messageBody shouldContain "\"accountId\":\"$accountId\""
        messageBody shouldContain "Congratulations"
        messageBody shouldContain goalName
    }

    @InterACtTest
    @MethodSource("goalFailedEvents")
    fun `should process goal failed event and send notification`(eventStimulus: AmqpMessage<GoalFailedEvent>) {
        // Given
        val accountId = eventStimulus.body.accountId
        val goalName = eventStimulus.body.goalName

        // Create a subscription for the account
        createTestSubscription(accountId, NotificationEventType.GOAL_FAILED)

        // When - Send the goal failed event
        testAmqpClient.send(
            GOAL_EXCHANGE_NAME,
            GOAL_FAILED_ROUTING_KEY,
            eventStimulus
        )

        // Then - Wait for the notification to be sent
        await().atMost(5, TimeUnit.SECONDS).until {
            // Check if a notification was created in the repository
            val pageable = PageRequest.of(0, 10)
            val notifications = notificationRepository.findByAccountId(accountId, pageable)
            !notifications.isEmpty()
        }

        // And - Verify that a notification was sent to RabbitMQ
        val message = waitForMessage(5)
        message shouldNotBe null

        // Convert the message body to a string and verify it contains the expected data
        val messageBody = String(message!!.body)
        messageBody shouldContain "\"eventType\":\"GOAL_FAILED\""
        messageBody shouldContain "\"accountId\":\"$accountId\""
        messageBody shouldContain "failed"
        messageBody shouldContain goalName
    }

    private fun createTestSubscription(
        accountId: String,
        eventType: NotificationEventType,
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    ): NotificationSubscription {
        val subscription = NotificationSubscription(
            id = id,
            accountId = accountId,
            eventType = eventType,
            active = true,
            createdAt = Instant.now(clock)
        )
        return subscriptionRepository.save(subscription)
    }

    private fun waitForMessage(timeout: Long): Message? {
        return rabbitTemplate.receive("test_queue", timeout * 1000)
    }
}
