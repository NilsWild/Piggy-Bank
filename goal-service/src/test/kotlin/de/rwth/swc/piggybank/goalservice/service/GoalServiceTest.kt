package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.CreateSavingsGoalRequest
import de.rwth.swc.piggybank.goalservice.dto.CreateSpendingLimitGoalRequest
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import de.rwth.swc.piggybank.goalservice.repository.SavingsGoalRepository
import de.rwth.swc.piggybank.goalservice.repository.SpendingLimitGoalRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.util.Optional
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GoalServiceTest {

    private lateinit var goalRepository: GoalRepository
    private lateinit var spendingLimitGoalRepository: SpendingLimitGoalRepository
    private lateinit var savingsGoalRepository: SavingsGoalRepository
    private lateinit var rabbitMQService: RabbitMQService
    private lateinit var goalService: GoalService
    private lateinit var clock: Clock

    private val accountId = "test-account-id"
    private val now = Instant.now()
    private val future = now.plus(Duration.ofDays(30))

    @BeforeEach
    fun setup() {
        goalRepository = mockk(relaxed = true)
        spendingLimitGoalRepository = mockk(relaxed = true)
        savingsGoalRepository = mockk(relaxed = true)
        rabbitMQService = mockk(relaxed = true)
        clock = Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC"))
        goalService = GoalServiceImpl(
            goalRepository = goalRepository,
            spendingLimitGoalRepository = spendingLimitGoalRepository,
            savingsGoalRepository = savingsGoalRepository,
            rabbitMQService = rabbitMQService,
            clock = clock
        )
    }

    @Test
    fun `should create spending limit goal`() {
        // Given
        val request = CreateSpendingLimitGoalRequest(
            name = "Grocery Spending Limit",
            description = "Limit grocery spending to 400 EUR per month",
            startDate = now,
            endDate = future,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery"
        )

        val savedGoal = SpendingLimitGoal(
            name = request.name,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            accountId = request.accountId,
            limit = request.limit,
            currencyCode = request.currencyCode,
            category = request.category,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )

        val goalSlot = slot<SpendingLimitGoal>()
        every { spendingLimitGoalRepository.save(capture(goalSlot)) } returns savedGoal

        // When
        val result = goalService.createSpendingLimitGoal(request)

        // Then
        goalSlot.captured.name shouldBe request.name
        goalSlot.captured.description shouldBe request.description
        goalSlot.captured.startDate shouldBe request.startDate
        goalSlot.captured.endDate shouldBe request.endDate
        goalSlot.captured.accountId shouldBe request.accountId
        goalSlot.captured.limit shouldBe request.limit
        goalSlot.captured.currencyCode shouldBe request.currencyCode
        goalSlot.captured.category shouldBe request.category

        result.name shouldBe request.name
        result.description shouldBe request.description
        result.startDate shouldBe request.startDate
        result.endDate shouldBe request.endDate
        result.accountId shouldBe request.accountId
        result.limit shouldBe request.limit
        result.currencyCode shouldBe request.currencyCode
        result.category shouldBe request.category
    }

    @Test
    fun `should create savings goal`() {
        // Given
        val request = CreateSavingsGoalRequest(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR"
        )

        val savedGoal = SavingsGoal(
            name = request.name,
            description = request.description,
            startDate = request.startDate,
            endDate = request.endDate,
            accountId = request.accountId,
            targetAmount = request.targetAmount,
            currencyCode = request.currencyCode,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )

        val goalSlot = slot<SavingsGoal>()
        every { savingsGoalRepository.save(capture(goalSlot)) } returns savedGoal

        // When
        val result = goalService.createSavingsGoal(request)

        // Then
        goalSlot.captured.name shouldBe request.name
        goalSlot.captured.description shouldBe request.description
        goalSlot.captured.startDate shouldBe request.startDate
        goalSlot.captured.endDate shouldBe request.endDate
        goalSlot.captured.accountId shouldBe request.accountId
        goalSlot.captured.targetAmount shouldBe request.targetAmount
        goalSlot.captured.currencyCode shouldBe request.currencyCode

        result.name shouldBe request.name
        result.description shouldBe request.description
        result.startDate shouldBe request.startDate
        result.endDate shouldBe request.endDate
        result.accountId shouldBe request.accountId
        result.targetAmount shouldBe request.targetAmount
        result.currencyCode shouldBe request.currencyCode
    }

    @Test
    fun `should get goal by ID`() {
        // Given
        val goalId = UUID.randomUUID()
        val goal = SpendingLimitGoal(
            name = "Grocery Spending Limit",
            description = "Limit grocery spending to 400 EUR per month",
            startDate = now,
            endDate = future,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )

        every { goalRepository.findById(goalId) } returns Optional.of(goal)

        // When
        val result = goalService.getGoalById(goalId)

        // Then
        result shouldBe goal
    }

    @Test
    fun `should get spending limit goal by ID`() {
        // Given
        val goalId = UUID.randomUUID()
        val goal = SpendingLimitGoal(
            name = "Grocery Spending Limit",
            description = "Limit grocery spending to 400 EUR per month",
            startDate = now,
            endDate = future,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )

        every { spendingLimitGoalRepository.findById(goalId) } returns Optional.of(goal)

        // When
        val result = goalService.getSpendingLimitGoalById(goalId)

        // Then
        result shouldBe goal
    }

    @Test
    fun `should get savings goal by ID`() {
        // Given
        val goalId = UUID.randomUUID()
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )

        every { savingsGoalRepository.findById(goalId) } returns Optional.of(goal)

        // When
        val result = goalService.getSavingsGoalById(goalId)

        // Then
        result shouldBe goal
    }

    @Test
    fun `should get goals by account ID`() {
        // Given
        val goals = listOf(
            SpendingLimitGoal(
                name = "Grocery Spending Limit",
                description = "Limit grocery spending to 400 EUR per month",
                startDate = now,
                endDate = future,
                accountId = accountId,
                limit = BigDecimal("400.00"),
                currencyCode = "EUR",
                category = "Grocery",
                createdAt = Instant.now(clock),
                updatedAt = Instant.now(clock)
            ),
            SavingsGoal(
                name = "Vacation Savings",
                description = "Save 1000 EUR for summer vacation",
                startDate = now,
                endDate = future,
                accountId = accountId,
                targetAmount = BigDecimal("1000.00"),
                currencyCode = "EUR",
                createdAt = Instant.now(clock),
                updatedAt = Instant.now(clock)
            )
        )

        every { goalRepository.findByAccountId(accountId) } returns goals

        // When
        val result = goalService.getGoalsByAccountId(accountId)

        // Then
        result shouldBe goals
    }

    @Test
    fun `should get goals by status`() {
        // Given
        val status = GoalStatus.ACTIVE
        val goals = listOf(
            SpendingLimitGoal(
                name = "Grocery Spending Limit",
                description = "Limit grocery spending to 400 EUR per month",
                startDate = now,
                endDate = future,
                accountId = accountId,
                limit = BigDecimal("400.00"),
                currencyCode = "EUR",
                category = "Grocery",
                createdAt = Instant.now(clock),
                updatedAt = Instant.now(clock)
            ),
            SavingsGoal(
                name = "Vacation Savings",
                description = "Save 1000 EUR for summer vacation",
                startDate = now,
                endDate = future,
                accountId = accountId,
                targetAmount = BigDecimal("1000.00"),
                currencyCode = "EUR",
                createdAt = Instant.now(clock),
                updatedAt = Instant.now(clock)
            )
        )

        every { goalRepository.findByStatus(status) } returns goals

        // When
        val result = goalService.getGoalsByStatus(status)

        // Then
        result shouldBe goals
    }

    @Test
    fun `should update goal status`() {
        // Given
        val goalId = UUID.randomUUID()
        val status = GoalStatus.ACHIEVED
        val goal = SpendingLimitGoal(
            name = "Grocery Spending Limit",
            description = "Limit grocery spending to 400 EUR per month",
            startDate = now,
            endDate = future,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )

        every { goalRepository.findById(goalId) } returns Optional.of(goal)
        every { goalRepository.save(any()) } returns goal
        justRun { rabbitMQService.sendGoalStatusEvent(any()) }

        // When
        val result = goalService.updateGoalStatus(goalId, status)

        // Then
        result shouldBe goal
        result?.status shouldBe status
        verify { goalRepository.save(goal) }
        verify { rabbitMQService.sendGoalStatusEvent(goal) }
    }

    @Test
    fun `should delete goal`() {
        // Given
        val goalId = UUID.randomUUID()
        every { goalRepository.existsById(goalId) } returns true

        // When
        val result = goalService.deleteGoal(goalId)

        // Then
        result shouldBe true
        verify { goalRepository.deleteById(goalId) }
    }

    @Test
    fun `should return false when deleting non-existent goal`() {
        // Given
        val goalId = UUID.randomUUID()
        every { goalRepository.existsById(goalId) } returns false

        // When
        val result = goalService.deleteGoal(goalId)

        // Then
        result shouldBe false
        verify(exactly = 0) { goalRepository.deleteById(any()) }
    }
}
