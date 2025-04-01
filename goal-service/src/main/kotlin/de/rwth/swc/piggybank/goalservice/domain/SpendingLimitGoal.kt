package de.rwth.swc.piggybank.goalservice.domain

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.math.BigDecimal
import java.time.LocalDateTime

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
@DiscriminatorValue("SPENDING_LIMIT")
class SpendingLimitGoal(
    name: String,
    description: String? = null,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    accountId: String,

    @Column(nullable = false)
    val limit: BigDecimal,

    @Column(nullable = false)
    val currencyCode: String,

    @Column(nullable = false)
    val category: String,

    @Column(nullable = false)
    var currentSpending: BigDecimal = BigDecimal.ZERO
) : Goal(
    name = name,
    description = description,
    type = GoalType.SPENDING_LIMIT,
    startDate = startDate,
    endDate = endDate,
    accountId = accountId
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
        classifications: List<String>
    ): Boolean {
        // Ignore if not for this account or if goal has ended
        if (this.accountId != accountId || hasEnded()) {
            return false
        }

        // Check if the transaction is relevant to this goal (matches the category)
        if (classifications.contains(category)) {
            // Only count outgoing transactions (negative amounts)
            if (transactionAmount < BigDecimal.ZERO) {
                // Add the absolute value of the transaction amount to the current spending
                currentSpending = currentSpending.add(transactionAmount.abs())
                updatedAt = LocalDateTime.now()

                // Check if the goal has been exceeded
                if (currentSpending > limit) {
                    updateStatus(GoalStatus.FAILED)
                    return true
                }
            }
        }

        // Check if the goal's timeframe has expired
        if (isExpired() && isActive()) {
            // If we're under the limit, the goal is achieved
            if (currentSpending <= limit) {
                updateStatus(GoalStatus.ACHIEVED)
                return true
            } else {
                updateStatus(GoalStatus.FAILED)
                return true
            }
        }

        return false
    }
}