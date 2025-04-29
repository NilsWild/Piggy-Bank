package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.AccountUpdatedEvent
import de.rwth.swc.piggybank.goalservice.dto.TransactionAmountDto
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountUpdateListenerTest {

    private lateinit var goalRepository: GoalRepository
    private lateinit var rabbitMQService: RabbitMQService
    private lateinit var transferClassificationCache: TransferClassificationCache
    private lateinit var accountUpdateListener: AccountUpdateListener
    private lateinit var clock: Clock

    private val accountId = "test-account-id"
    private val fixedInstant = Instant.parse("2023-01-01T12:00:00Z")
    private val now = fixedInstant
    private val future = now.plus(Duration.ofDays(30))
    private val transactionId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        goalRepository = mockk(relaxed = true)
        rabbitMQService = mockk(relaxed = true)
        transferClassificationCache = mockk(relaxed = true)
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
        accountUpdateListener = AccountUpdateListener(
            goalRepository = goalRepository,
            rabbitMQService = rabbitMQService,
            transferClassificationCache = transferClassificationCache,
            clock = clock
        )
    }

    @Test
    fun `should process account update event with active goals`() {
        // Given
        val event = createAccountUpdatedEvent()
        val activeGoals = listOf(
            SpendingLimitGoal(
                name = "Grocery Spending Limit",
                description = "Limit grocery spending to 400 EUR per month",
                startDate = now,
                endDate = future,
                accountId = accountId,
                limit = BigDecimal("400.00"),
                currencyCode = "EUR",
                category = "Grocery",
                createdAt = now,
                updatedAt = now
            )
        )

        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } returns activeGoals

        // When
        accountUpdateListener.handleAccountUpdatedEvent(event)

        // Then
        verify { transferClassificationCache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = "-50.00",
            type = "DEBIT",
            purpose = "Grocery shopping"
        ) }
        verify { transferClassificationCache.mapTransactionToTransfer(transactionId, transferId) }
        verify { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) }

        verify(exactly = 0) { goalRepository.saveAll(any<List<SpendingLimitGoal>>()) }
        verify(exactly = 0) { rabbitMQService.sendGoalStatusEvent(any()) }
    }

    @Test
    fun `should process account update event with no active goals`() {
        // Given
        val event = createAccountUpdatedEvent()
        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } returns emptyList()

        // When
        accountUpdateListener.handleAccountUpdatedEvent(event)

        // Then
        verify { transferClassificationCache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = "-50.00",
            type = "DEBIT",
            purpose = "Grocery shopping"
        ) }
        verify { transferClassificationCache.mapTransactionToTransfer(transactionId, transferId) }
        verify { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) }
        verify(exactly = 0) { goalRepository.saveAll(any<List<SpendingLimitGoal>>()) }
        verify(exactly = 0) { rabbitMQService.sendGoalStatusEvent(any()) }
    }

    @Test
    fun `should process account update event that changes goal status`() {
        // Given
        // Create an event with a positive transaction amount for a SavingsGoal
        val event = createAccountUpdatedEventWithPositiveAmount()

        // Create a SavingsGoal with a target amount that will be reached when the transaction amount is added
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            currentAmount = BigDecimal("950.00"), // Current amount is close to target
            createdAt = now,
            updatedAt = now
        )
        val activeGoals = listOf(goal)

        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } returns activeGoals

        // When
        accountUpdateListener.handleAccountUpdatedEvent(event)

        // Then
        verify { transferClassificationCache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = "100.00",
            type = "CREDIT",
            purpose = "Salary payment"
        ) }
        verify { transferClassificationCache.mapTransactionToTransfer(transactionId, transferId) }
        verify { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) }

        // The goal should be saved and a status event should be sent because the goal is achieved
        verify { goalRepository.saveAll(listOf(goal)) }
        verify { rabbitMQService.sendGoalStatusEvent(goal) }
    }

    @Test
    fun `should handle exceptions during event processing`() {
        // Given
        val event = createAccountUpdatedEvent()
        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } throws RuntimeException("Test exception")

        // When/Then
        assertThrows<RuntimeException> {
            accountUpdateListener.handleAccountUpdatedEvent(event)
        }
    }

    private val transferId = UUID.randomUUID()

    private fun createAccountUpdatedEvent(): AccountUpdatedEvent {
        return AccountUpdatedEvent(
            eventType = "ACCOUNT_UPDATED",
            accountId = accountId,
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
    }

    private fun createAccountUpdatedEventWithPositiveAmount(): AccountUpdatedEvent {
        return AccountUpdatedEvent(
            eventType = "ACCOUNT_UPDATED",
            accountId = accountId,
            accountType = "CHECKING",
            accountIdentifier = "DE123456789",
            value = "1050.00",
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