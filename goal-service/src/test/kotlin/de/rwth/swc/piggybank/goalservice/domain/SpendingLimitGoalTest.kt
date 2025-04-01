package de.rwth.swc.piggybank.goalservice.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpendingLimitGoalTest {

    private val accountId = "test-account-id"
    private val now = LocalDateTime.now()
    private val future = now.plusDays(30)
    private val past = now.minusDays(1)

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
            category = "Grocery"
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery")
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
            category = "Grocery"
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Hotel booking",
            classifications = listOf("Holiday")
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
            category = "Grocery"
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("50.00"),
            transactionType = "CREDIT",
            transactionPurpose = "Aldi refund",
            classifications = listOf("Grocery")
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
            category = "Grocery"
        )

        // When
        goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-300.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery")
        )

        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-150.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Lidl purchase",
            classifications = listOf("Grocery")
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
            startDate = past.minusDays(30),
            endDate = past,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            currentSpending = BigDecimal("350.00")
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-10.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery")
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
            startDate = past.minusDays(30),
            endDate = past,
            accountId = accountId,
            limit = BigDecimal("400.00"),
            currencyCode = "EUR",
            category = "Grocery",
            currentSpending = BigDecimal("450.00")
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-10.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery")
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
            category = "Grocery"
        )

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = "different-account-id",
            transactionAmount = BigDecimal("-50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery")
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
            category = "Grocery"
        )
        goal.updateStatus(GoalStatus.ACHIEVED)

        // When
        val statusChanged = goal.processAccountUpdate(
            accountId = accountId,
            transactionAmount = BigDecimal("-50.00"),
            transactionType = "DEBIT",
            transactionPurpose = "Aldi purchase",
            classifications = listOf("Grocery")
        )

        // Then
        goal.currentSpending shouldBe BigDecimal.ZERO
        statusChanged shouldBe false
    }
}