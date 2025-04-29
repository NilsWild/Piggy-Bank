package de.rwth.swc.piggybank.goalservice.domain

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

/**
 * A goal to limit spending on a specific category.
 * This goal is achieved if the total spending in the specified category is less than or equal to the limit
 * by the end of the timeframe.
 *
 * @property limit The maximum amount to spend
 * @property currencyCode The currency code of the limit
 * @property category The category to track (e.g., "Grocery", "Holiday")
 * @property currentSpending The current amount spent in the category
 */
@Entity
class SpendingLimitGoal(
    name: String,
    description: String? = null,
    startDate: Instant,
    endDate: Instant,
    accountId: String,

    @Column(nullable = false)
    val limit: BigDecimal,

    @Column(nullable = false)
    val currencyCode: String,

    @Column(nullable = false)
    val category: String,

    @Column(nullable = false)
    var currentSpending: BigDecimal = BigDecimal.ZERO,

    createdAt: Instant,
    updatedAt: Instant
) : Goal(
    name = name,
    description = description,
    type = GoalType.SPENDING_LIMIT,
    startDate = startDate,
    endDate = endDate,
    accountId = accountId,
    createdAt = createdAt,
    updatedAt = updatedAt
) {

    /**
     * Processes an account update event.
     * Updates the current spending if the transaction is relevant to this goal.
     *
     * @param accountId The ID of the account
     * @param transactionAmount The amount of the transaction
     * @param transactionType The type of the transaction
     * @param transactionPurpose The purpose of the transaction
     * @param classifications The classifications of the transaction
     * @return true if the goal's status changed, false otherwise
     */
    override fun processAccountUpdate(
        accountId: String,
        transactionAmount: BigDecimal,
        transactionType: String,
        transactionPurpose: String,
        classifications: List<String>,
        clock: Clock
    ): Boolean {
        // Ignore if not for this account
        if (this.accountId != accountId) {
            return false
        }

        // Ignore if the goal has ended
        if (hasEnded()) {
            return false
        }

        // Save the original status to check if it changes
        val originalStatus = status

        // Update the current spending if the transaction is relevant
        if (classifications.contains(category) && transactionType == "DEBIT") {
            currentSpending = currentSpending.add(transactionAmount.abs())
            updatedAt = Instant.now(clock)

            // Check if the goal has been exceeded
            if (currentSpending > limit && isActive()) {
                updateStatus(GoalStatus.FAILED, clock)
            }
        }

        // Check if the goal's timeframe has expired
        if (isExpired(clock) && isActive()) {
            // If we're under the limit, the goal is achieved
            if (currentSpending <= limit) {
                updateStatus(GoalStatus.ACHIEVED, clock)
            } else {
                updateStatus(GoalStatus.FAILED, clock)
            }
        }

        // Return true if the status changed, false otherwise
        return status != originalStatus
    }
}