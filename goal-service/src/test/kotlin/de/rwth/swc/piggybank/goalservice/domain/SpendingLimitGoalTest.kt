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
class SpendingLimitGoalTest {

    private val accountId = "test-account-id"
    private val clock = Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC"))
    private val now = Instant.now(clock)
    private val future = now.plus(Duration.ofDays(30))
    private val past = now.minus(Duration.ofDays(1))

    @Test
    fun `should update current spending when transaction matches category`() {
        // Given
        val goal = SpendingLimitGoal(
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

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal("50.00")
        statusChanged shouldBe false
    }

    @Test
    fun `should not update current spending when transaction does not match category`() {
        // Given
        val goal = SpendingLimitGoal(
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

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Hotel booking",
            classifications = listOf("Holiday"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }

    @Test
    fun `should not update current spending for positive transaction amounts`() {
        // Given
        val goal = SpendingLimitGoal(
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

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("50.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Aldi refund",
            classifications = listOf("Grocery"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }

    @Test
    fun `should fail goal when spending exceeds limit`() {
        // Given
        val goal = SpendingLimitGoal(
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

        // When
        goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("300.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery"),
            clock = clock
        )

        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("150.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Lidl purchase",
            classifications = listOf("Grocery"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal("450.00")
        goal.status shouldBe GoalStatus.FAILED
        statusChanged shouldBe true
    }

    @Test
    fun `should achieve goal when timeframe expires and spending is under limit`() {
        // Given
        val goal = SpendingLimitGoal(
            name = "Grocery Spending Limit",
            description = "Limit grocery spending to 400 EUR per month",
            startDate = past.minus(Duration.ofDays(30)),
            endDate = past,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            currentSpending = BigDecimal("350.00"),
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("10.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal("360.00")
        goal.status shouldBe GoalStatus.ACHIEVED
        statusChanged shouldBe true
    }

    @Test
    fun `should fail goal when timeframe expires and spending exceeds limit`() {
        // Given
        val goal = SpendingLimitGoal(
            name = "Grocery Spending Limit",
            description = "Limit grocery spending to 400 EUR per month",
            startDate = past.minus(Duration.ofDays(30)),
            endDate = past,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            currentSpending = BigDecimal("450.00"),
            createdAt = now,
            updatedAt = now
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("10.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal("460.00")
        goal.status shouldBe GoalStatus.FAILED
        statusChanged shouldBe true
    }

    @Test
    fun `should not process updates for different account`() {
        // Given
        val goal = SpendingLimitGoal(
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

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = "different-account-id",
            transactionAmount = BigDecimal("50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }

    @Test
    fun `should not process updates for ended goals`() {
        // Given
        val goal = SpendingLimitGoal(
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
        goal.updateStatus(GoalStatus.ACHIEVED, clock)

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery"),
            clock = clock
        )

        // Then
        goal.currentSpending shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }
}