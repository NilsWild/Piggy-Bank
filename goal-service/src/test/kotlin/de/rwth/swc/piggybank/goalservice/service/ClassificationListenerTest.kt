package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.ClassificationEvent
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassificationListenerTest {

    private lateinit var goalRepository: GoalRepository
    private lateinit var rabbitMQService: RabbitMQService
    private lateinit var transferClassificationCache: TransferClassificationCache
    private lateinit var classificationListener: ClassificationListener

    private val accountId = "test-account-id"
    private val now = LocalDateTime.now()
    private val future = now.plusDays(30)
    private val transferId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        goalRepository = mockk(relaxed = true)
        rabbitMQService = mockk(relaxed = true)
        transferClassificationCache = mockk(relaxed = true)
        classificationListener = ClassificationListener(
            goalRepository = goalRepository,
            rabbitMQService = rabbitMQService,
            transferClassificationCache = transferClassificationCache
        )

        // Setup default behavior for the cache
        every { transferClassificationCache.getAccountId(transferId) } returns accountId
        every { transferClassificationCache.getAmount(transferId) } returns "-50.00"
        every { transferClassificationCache.getType(transferId) } returns "DEBIT"
        every { transferClassificationCache.getPurpose(transferId) } returns "Grocery shopping"
    }

    @Test
    fun `should process classification event with active goals`() {
        // Given
        val event = createClassificationEvent()
        val activeGoals = listOf(
            SpendingLimitGoal(
                name = "Grocery Spending Limit",
                description = "Limit grocery spending to 400 EUR per month",
                startDate = now,
                endDate = future,
                accountId = accountId,
                limit = BigDecimal("400.00"),
                currencyCode = "EUR",
                category = "Grocery"
            )
        )

        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } returns activeGoals

        // When
        classificationListener.handleClassificationEvent(event)

        // Then
        verify { transferClassificationCache.getAccountId(transferId) }
        verify { transferClassificationCache.getAmount(transferId) }
        verify { transferClassificationCache.getType(transferId) }
        verify { transferClassificationCache.getPurpose(transferId) }
        verify { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) }

        verify(exactly = 0) { goalRepository.saveAll(any<List<SpendingLimitGoal>>()) }
        verify(exactly = 0) { rabbitMQService.sendGoalStatusEvent(any()) }
        verify { transferClassificationCache.removeTransferInfo(transferId) }
    }

    @Test
    fun `should process classification event with no active goals`() {
        // Given
        val event = createClassificationEvent()
        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } returns emptyList()

        // When
        classificationListener.handleClassificationEvent(event)

        // Then
        verify { transferClassificationCache.getAccountId(transferId) }
        verify { transferClassificationCache.getAmount(transferId) }
        verify { transferClassificationCache.getType(transferId) }
        verify { transferClassificationCache.getPurpose(transferId) }
        verify { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) }
        verify(exactly = 0) { goalRepository.saveAll(any<List<SpendingLimitGoal>>()) }
        verify(exactly = 0) { rabbitMQService.sendGoalStatusEvent(any()) }
        verify { transferClassificationCache.removeTransferInfo(transferId) }
    }

    @Test
    fun `should process classification event that changes goal status`() {
        // Given
        val event = createClassificationEvent()
        // Create a goal with a limit of 400 EUR and a current spending of 380 EUR
        // The transaction amount is -50 EUR, which will exceed the limit and cause the goal to fail
        val goal = SpendingLimitGoal(
            name = "Grocery Spending Limit",
            description = "Limit grocery spending to 400 EUR per month",
            startDate = now,
            endDate = future,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            currentSpending = BigDecimal("380.00")
        )
        val activeGoals = listOf(goal)

        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } returns activeGoals

        // When
        classificationListener.handleClassificationEvent(event)

        // Then
        verify { transferClassificationCache.getAccountId(transferId) }
        verify { transferClassificationCache.getAmount(transferId) }
        verify { transferClassificationCache.getType(transferId) }
        verify { transferClassificationCache.getPurpose(transferId) }
        verify { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) }

        verify { goalRepository.saveAll(listOf(goal)) }
        verify { rabbitMQService.sendGoalStatusEvent(goal) }
        verify { transferClassificationCache.removeTransferInfo(transferId) }
    }

    @Test
    fun `should handle missing transfer information`() {
        // Given
        val event = createClassificationEvent()
        every { transferClassificationCache.getAccountId(transferId) } returns null

        // When
        classificationListener.handleClassificationEvent(event)

        // Then
        verify { transferClassificationCache.getAccountId(transferId) }
        verify(exactly = 0) { goalRepository.findByAccountIdAndStatus(any(), any()) }
        verify(exactly = 0) { goalRepository.saveAll(any<List<SpendingLimitGoal>>()) }
        verify(exactly = 0) { rabbitMQService.sendGoalStatusEvent(any()) }
    }

    @Test
    fun `should handle exceptions during event processing`() {
        // Given
        val event = createClassificationEvent()
        every { goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE) } throws RuntimeException("Test exception")

        // When/Then
        assertThrows<RuntimeException> {
            classificationListener.handleClassificationEvent(event)
        }
    }

    private fun createClassificationEvent(): ClassificationEvent {
        return ClassificationEvent(
            transferId = transferId,
            classifications = listOf("Grocery")
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
