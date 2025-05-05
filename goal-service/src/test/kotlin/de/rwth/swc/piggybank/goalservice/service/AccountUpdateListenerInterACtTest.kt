package de.rwth.swc.piggybank.goalservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.amqp.TestAmqpClient
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.amqp.AmqpMessage
import de.interact.domain.testobservation.config.Configuration
import de.interact.junit.jupiter.annotation.InterACtTest
import de.rwth.swc.piggybank.goalservice.GoalServiceApplication
import de.rwth.swc.piggybank.goalservice.config.AmqpTestConfig
import de.rwth.swc.piggybank.goalservice.config.InterACtConfig
import de.rwth.swc.piggybank.goalservice.config.RabbitMQConfig
import de.rwth.swc.piggybank.goalservice.config.RabbitMQTestConfig
import de.rwth.swc.piggybank.goalservice.config.TestClockConfig
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.AccountUpdatedEvent
import de.rwth.swc.piggybank.goalservice.dto.TransactionAmountDto
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilNotNull

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
    private lateinit var goalRepository: GoalRepository

    @Autowired
    private lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        goalRepository.deleteAll()
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
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()

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
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()

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
}
