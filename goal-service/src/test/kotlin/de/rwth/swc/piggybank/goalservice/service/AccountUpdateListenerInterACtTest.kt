package de.rwth.swc.piggybank.goalservice.service

import de.interact.amqp.TestAmqpClient
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.amqp.AmqpMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountUpdatedEvent
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionAmountDto
import de.rwth.swc.piggybank.goalservice.GoalServiceApplication
import de.rwth.swc.piggybank.goalservice.config.*
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import de.rwth.swc.piggybank.goalservice.util.RabbitMQTestUtils
import de.rwth.swc.piggybank.transferclassifier.domain.ClassificationResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@SpringBootTest(
    classes = [GoalServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class, TestClockConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [RabbitMQTestConfig.Initializer::class])
class AccountUpdateListenerInterACtTest {

    @Autowired
    private lateinit var rabbitTemplate: TestAmqpClient

    @Autowired
    @Qualifier("rabbitTemplate")
    private lateinit var rabbitTemplateForReceiving: RabbitTemplate

    @Autowired
    private lateinit var goalRepository: GoalRepository

    @Autowired
    private lateinit var transferClassificationCache: TransferClassificationCache

    @Autowired
    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        goalRepository.deleteAll()
        transferClassificationCache.clear()
    }

    @InterACtTest
    @MethodSource("accountUpdatedEventForSavingsGoal")
    fun `should process account update event for savings goal`(eventStimulus: AmqpMessage<AccountUpdatedEvent>) {
        // Create a savings goal
        val accountId = eventStimulus.body.accountId
        val goal = createTestSavingsGoal(accountId)

        // Send the account update event
        rabbitTemplate.send(
            RabbitMQConfig.ACCOUNT_EXCHANGE_NAME,
            RabbitMQConfig.ACCOUNT_UPDATED_ROUTING_KEY,
            eventStimulus
        )

        // Wait for the event to be processed
        await().atMost(5, TimeUnit.SECONDS).until {
            val updatedGoal = goalRepository.findById(goal.id).orElse(null) as? SavingsGoal
            updatedGoal?.currentAmount == BigDecimal("100.00")
        }

        // Verify the goal was updated
        val updatedGoal = goalRepository.findById(goal.id).orElse(null)
        updatedGoal shouldNotBe null
        updatedGoal as SavingsGoal
        updatedGoal.currentAmount shouldBe BigDecimal("100.00")
    }

    @InterACtTest
    @MethodSource("accountUpdatedEventForSavingsGoalAchievement")
    fun `should achieve savings goal when target amount is reached`(eventStimulus: AmqpMessage<AccountUpdatedEvent>) {
        // Create a savings goal that's close to being achieved
        val accountId = eventStimulus.body.accountId
        val goal = SavingsGoal(
            name = "Test Savings Goal",
            description = "Test Description",
            startDate = Instant.now(clock),
            endDate = Instant.now(clock).plusSeconds(60 * 60 * 24 * 30),
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            currentAmount = BigDecimal("900.00"),
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )
        goalRepository.save(goal)

        // Send the account update event with a large enough amount to achieve the goal
        rabbitTemplate.send(
            RabbitMQConfig.ACCOUNT_EXCHANGE_NAME,
            RabbitMQConfig.ACCOUNT_UPDATED_ROUTING_KEY,
            eventStimulus
        )

        // Wait for the event to be processed
        await().atMost(5, TimeUnit.SECONDS).until {
            val updatedGoal = goalRepository.findById(goal.id).orElse(null) as? SavingsGoal
            updatedGoal?.status == GoalStatus.ACHIEVED && updatedGoal.currentAmount == BigDecimal("1000.00")
        }

        // Verify the goal was updated and achieved
        val updatedGoal = goalRepository.findById(goal.id).orElse(null)
        updatedGoal shouldNotBe null
        updatedGoal as SavingsGoal
        updatedGoal.currentAmount shouldBe BigDecimal("1000.00")
        updatedGoal.status shouldBe GoalStatus.ACHIEVED

        // Verify that a GOAL_ACHIEVED event was sent to RabbitMQ
        val message = RabbitMQTestUtils.waitForEventType(rabbitTemplateForReceiving, "GOAL_ACHIEVED", 2, TimeUnit.SECONDS)
        message shouldNotBe null

        // Convert the message body to a string and verify it contains the expected data
        val messageBody = String(message!!.body)
        messageBody shouldContain "\"eventType\":\"GOAL_ACHIEVED\""
        messageBody shouldContain "\"goalId\":\"${goal.id}\""
        messageBody shouldContain "\"goalName\":\"${goal.name}\""
        messageBody shouldContain "\"accountId\":\"$accountId\""
        messageBody shouldContain "\"goalStatus\":\"ACHIEVED\""
    }

    @InterACtTest
    @MethodSource("accountUpdatedEventForSpendingLimitGoal")
    fun `should process account update event for spending limit goal`(eventStimulus: AmqpMessage<AccountUpdatedEvent>){
        // Create a spending limit goal
        val accountId = eventStimulus.body.accountId
        val goal = createTestSpendingLimitGoal(accountId)

        // Extract transfer ID from the event
        val transferId = UUID.fromString(eventStimulus.body.transferId)

        // Store classifications in the cache
        transferClassificationCache.storeClassifications(
            transferId = transferId,
            classifications = listOf("Grocery")
        )

        // Send only the account update event via RabbitMQ
        rabbitTemplate.send(
            RabbitMQConfig.ACCOUNT_EXCHANGE_NAME,
            RabbitMQConfig.ACCOUNT_UPDATED_ROUTING_KEY,
            eventStimulus
        )

        // Wait for the goal to be updated
        await().atMost(5, TimeUnit.SECONDS).until {
            val updatedGoal = goalRepository.findById(goal.id).orElse(null) as? SpendingLimitGoal
            updatedGoal?.currentSpending == BigDecimal("50.00")
        }

        // Verify the goal was updated
        val updatedGoal = goalRepository.findById(goal.id).orElse(null)
        updatedGoal shouldNotBe null
        updatedGoal as SpendingLimitGoal
        updatedGoal.currentSpending shouldBe BigDecimal("50.00")
    }

    @InterACtTest
    @MethodSource("classificationEventForSpendingLimitGoal")
    fun `should process classification event when transfer is already in cache`(eventStimulus: AmqpMessage<ClassificationResult>){
        // Create a spending limit goal
        val accountId = "test-account-123" // Same as in accountUpdatedEventForSpendingLimitGoal
        val goal = createTestSpendingLimitGoal(accountId)

        // Extract transfer ID from the event
        val transferId = eventStimulus.body.transferId

        // Set up the transfer in the cache first
        transferClassificationCache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = "-50.00", // Same as in accountUpdatedEventForSpendingLimitGoal
            type = "DEBIT", // Same as in accountUpdatedEventForSpendingLimitGoal
            purpose = "Grocery shopping" // Same as in accountUpdatedEventForSpendingLimitGoal
        )

        // Send only the classification event via RabbitMQ
        rabbitTemplate.send(
            RabbitMQConfig.CLASSIFICATION_EXCHANGE_NAME,
            RabbitMQConfig.CLASSIFICATION_ROUTING_KEY,
            eventStimulus
        )

        // Wait for the goal to be updated
        await().atMost(5, TimeUnit.SECONDS).until {
            val updatedGoal = goalRepository.findById(goal.id).orElse(null) as? SpendingLimitGoal
            updatedGoal?.currentSpending == BigDecimal("50.00")
        }

        // Verify the goal was updated
        val updatedGoal = goalRepository.findById(goal.id).orElse(null)
        updatedGoal shouldNotBe null
        updatedGoal as SpendingLimitGoal
        updatedGoal.currentSpending shouldBe BigDecimal("50.00")
    }

    @InterACtTest
    @MethodSource("classificationEventForExceedingSpendingLimitGoal")
    fun `should fail spending limit goal when spending exceeds limit`(eventStimulus: AmqpMessage<ClassificationResult>){
        // Create a spending limit goal with a low limit
        val accountId = "test-account-123"
        val goal = createTestSpendingLimitGoalWithLowLimit(accountId)

        // Extract transfer ID from the event
        val transferId = eventStimulus.body.transferId

        // Set up the transfer in the cache with an amount that exceeds the limit
        transferClassificationCache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = "120.00", // This exceeds the limit of 100.00
            type = "DEBIT",
            purpose = "Grocery shopping"
        )

        // Send only the classification event via RabbitMQ
        rabbitTemplate.send(
            RabbitMQConfig.CLASSIFICATION_EXCHANGE_NAME,
            RabbitMQConfig.CLASSIFICATION_ROUTING_KEY,
            eventStimulus
        )

        // Wait for the goal to be updated and marked as failed
        await().atMost(5, TimeUnit.SECONDS).until {
            val updatedGoal = goalRepository.findById(goal.id).orElse(null) as? SpendingLimitGoal
            updatedGoal?.currentSpending == BigDecimal("120.00") && updatedGoal.status == GoalStatus.FAILED
        }

        // Verify the goal was updated and marked as failed
        val updatedGoal = goalRepository.findById(goal.id).orElse(null)
        updatedGoal shouldNotBe null
        updatedGoal as SpendingLimitGoal
        updatedGoal.currentSpending shouldBe BigDecimal("120.00")
        updatedGoal.status shouldBe GoalStatus.FAILED

        // Verify that a GOAL_FAILED event was sent to RabbitMQ
        val message = RabbitMQTestUtils.waitForEventType(rabbitTemplateForReceiving, "GOAL_FAILED", 2, TimeUnit.SECONDS)
        message shouldNotBe null

        // Convert the message body to a string and verify it contains the expected data
        val messageBody = String(message!!.body)
        messageBody shouldContain "\"eventType\":\"GOAL_FAILED\""
        messageBody shouldContain "\"goalId\":\"${goal.id}\""
        messageBody shouldContain "\"goalName\":\"${goal.name}\""
        messageBody shouldContain "\"accountId\":\"$accountId\""
        messageBody shouldContain "\"goalStatus\":\"FAILED\""
    }

    fun accountUpdatedEventForSavingsGoal(): Stream<Arguments> {
        val transactionId = UUID.fromString("7a2c259a-f63a-4951-a876-a2e8a7d1399b")
        val transferId = UUID.fromString("df522f72-a23c-439b-bf8d-2cc0e7257551")

        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.ACCOUNT_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.ACCOUNT_UPDATED_ROUTING_KEY
                    ),
                    AccountUpdatedEvent(
                        eventType = "ACCOUNT_UPDATED",
                        accountId = "test-account-123",
                        accountType = "CHECKING",
                        accountIdentifier = "DE123456789",
                        value = "1100.00",
                        currencyCode = "EUR",
                        transactionId = transactionId.toString(),
                        transferId = transferId.toString(),
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

    fun accountUpdatedEventForSavingsGoalAchievement(): Stream<Arguments> {
        val transactionId = UUID.fromString("7a2c259a-f63a-4951-a876-a2e8a7d1399b")
        val transferId = UUID.fromString("df522f72-a23c-439b-bf8d-2cc0e7257551")

        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.ACCOUNT_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.ACCOUNT_UPDATED_ROUTING_KEY
                    ),
                    AccountUpdatedEvent(
                        eventType = "ACCOUNT_UPDATED",
                        accountId = "test-account-123",
                        accountType = "CHECKING",
                        accountIdentifier = "DE123456789",
                        value = "1000.00",
                        currencyCode = "EUR",
                        transactionId = transactionId.toString(),
                        transferId = transferId.toString(),
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

    fun accountUpdatedEventForSpendingLimitGoal(): Stream<Arguments> {
        val transactionId = UUID.fromString("7a2c259a-f63a-4951-a876-a2e8a7d1399b")
        val transferId = UUID.fromString("df522f72-a23c-439b-bf8d-2cc0e7257551")

        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.ACCOUNT_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.ACCOUNT_UPDATED_ROUTING_KEY
                    ),
                    AccountUpdatedEvent(
                        eventType = "ACCOUNT_UPDATED",
                        accountId = "test-account-123",
                        accountType = "CHECKING",
                        accountIdentifier = "DE123456789",
                        value = "950.00",
                        currencyCode = "EUR",
                        transactionId = transactionId.toString(),
                        transferId = transferId.toString(),
                        transactionAmount = TransactionAmountDto(
                            value = "-50.00",
                            currencyCode = "EUR"
                        ),
                        transactionType = "DEBIT",
                        transactionPurpose = "Grocery shopping"
                    )
                )
            )
        )
    }

    fun classificationEventForSpendingLimitGoal(): Stream<Arguments> {
        // Use a fixed transferId for consistency
        val transferId = UUID.fromString("8f7e6d5c-4b3a-2a1b-0c9d-8e7f6a5b4c3d")

        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.CLASSIFICATION_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.CLASSIFICATION_ROUTING_KEY
                    ),
                    ClassificationResult(
                        transferId = transferId,
                        classifications = listOf("Grocery")
                    )
                )
            )
        )
    }

    fun classificationEventForExceedingSpendingLimitGoal(): Stream<Arguments> {
        // Use a fixed transferId for consistency
        val transferId = UUID.fromString("9f8e7d6c-5b4a-3a2b-1c0d-9e8f7a6b5c4e")

        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.CLASSIFICATION_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.CLASSIFICATION_ROUTING_KEY
                    ),
                    ClassificationResult(
                        transferId = transferId,
                        classifications = listOf("Grocery")
                    )
                )
            )
        )
    }

    private fun createTestSavingsGoal(accountId: String): SavingsGoal {
        val now = Instant.now(clock)
        val future = now.plusSeconds(60 * 60 * 24 * 30) // 30 days in the future

        val goal = SavingsGoal(
            name = "Test Savings Goal",
            description = "Test Description",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            currentAmount = BigDecimal.ZERO,
            createdAt = now,
            updatedAt = now
        )

        return goalRepository.save(goal)
    }

    private fun createTestSpendingLimitGoal(accountId: String): SpendingLimitGoal {
        val now = Instant.now(clock)
        val future = now.plusSeconds(60 * 60 * 24 * 30) // 30 days in the future

        val goal = SpendingLimitGoal(
            name = "Test Spending Limit Goal",
            description = "Test Description",
            startDate = now,
            endDate = future,
            accountId = accountId,
            limit = BigDecimal("100.00"),
            currencyCode = "EUR",
            category = "Grocery",
            currentSpending = BigDecimal.ZERO,
            createdAt = now,
            updatedAt = now
        )

        return goalRepository.save(goal)
    }

    private fun createTestSpendingLimitGoalWithLowLimit(accountId: String): SpendingLimitGoal {
        val now = Instant.now(clock)
        val future = now.plusSeconds(60 * 60 * 24 * 30) // 30 days in the future

        val goal = SpendingLimitGoal(
            name = "Test Spending Limit Goal with Low Limit",
            description = "Test Description for a goal that will be exceeded",
            startDate = now,
            endDate = future,
            accountId = accountId,
            limit = BigDecimal("100.00"), // Low limit that will be exceeded
            currencyCode = "EUR",
            category = "Grocery",
            currentSpending = BigDecimal.ZERO,
            createdAt = now,
            updatedAt = now
        )

        return goalRepository.save(goal)
    }
}
