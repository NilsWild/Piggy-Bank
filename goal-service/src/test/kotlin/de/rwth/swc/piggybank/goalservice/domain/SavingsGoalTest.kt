package de.rwth.swc.piggybank.goalservice.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SavingsGoalTest {

    private val accountId = "test-account-id"
    private val clock = Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC"))
    private val now = Instant.now(clock)
    private val future = now.plus(Duration.ofDays(30))
    private val past = now.minus(Duration.ofDays(1))

    @Test
    fun `should update current amount when transaction is positive`() {
        // Given
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("200.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Salary",
            classifications = emptyList(),
            clock = clock
        )

        // Then
        goal.currentAmount shouldBe BigDecimal("200.00")
        statusChanged shouldBe false
    }

    @Test
    fun `should not update current amount when transaction is negative`() {
        // Given
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Grocery shopping",
            classifications = emptyList(),
            clock = clock
        )

        // Then
        goal.currentAmount shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }

    @Test
    fun `should achieve goal when current amount reaches target amount`() {
        // Given
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            currentAmount = BigDecimal("800.00"),
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("300.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Salary",
            classifications = emptyList(),
            clock = clock
        )

        // Then
        goal.currentAmount shouldBe BigDecimal("1100.00")
        goal.status shouldBe GoalStatus.ACHIEVED
        statusChanged shouldBe true
    }

    @Test
    fun `should achieve goal when timeframe expires and current amount exceeds target amount`() {
        // Given
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = past.minus(Duration.ofDays(30)),
            endDate = past,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            currentAmount = BigDecimal("1100.00"),
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("100.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Salary",
            classifications = emptyList(),
            clock = clock
        )

        // Then
        goal.currentAmount shouldBe BigDecimal("1200.00")
        goal.status shouldBe GoalStatus.ACHIEVED
        statusChanged shouldBe true
    }

    @Test
    fun `should fail goal when timeframe expires and current amount is less than target amount`() {
        // Given
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = past.minus(Duration.ofDays(30)),
            endDate = past,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            currentAmount = BigDecimal("800.00"),
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("100.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Salary",
            classifications = emptyList(),
            clock = clock
        )

        // Then
        goal.currentAmount shouldBe BigDecimal("900.00")
        goal.status shouldBe GoalStatus.FAILED
        statusChanged shouldBe true
    }

    @Test
    fun `should not process updates for different account`() {
        // Given
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = "different-account-id",
            transactionAmount = BigDecimal("200.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Salary",
            classifications = emptyList(),
            clock = clock
        )

        // Then
        goal.currentAmount shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }

    @Test
    fun `should not process updates for ended goals`() {
        // Given
        val goal = SavingsGoal(
            name = "Vacation Savings",
            description = "Save 1000 EUR for summer vacation",
            startDate = now,
            endDate = future,
            accountId = accountId,
            targetAmount = BigDecimal("1000.00"),
            currencyCode = "EUR",
            createdAt = now,
            updatedAt = now
        )
        goal.updateStatus(GoalStatus.ACHIEVED, clock)

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("200.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Salary",
            classifications = emptyList(),
            clock = clock
        )

        // Then
        goal.currentAmount shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }
}