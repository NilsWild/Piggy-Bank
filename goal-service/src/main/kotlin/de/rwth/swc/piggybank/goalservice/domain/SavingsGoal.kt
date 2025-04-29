package de.rwth.swc.piggybank.goalservice.domain

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

/**
 * A goal to save a certain amount of money.
 * This goal is achieved if the total savings reach or exceed the target amount by the end of the timeframe.
 *
 * @property targetAmount The amount to save
 * @property currencyCode The currency code of the target amount
 * @property currentAmount The current amount saved
 */
@Entity
class SavingsGoal(
    name: String,
    description: String? = null,
    startDate: Instant,
    endDate: Instant,
    accountId: String,

    @Column(nullable = false)
    val targetAmount: BigDecimal,

    @Column(nullable = false)
    val currencyCode: String,

    @Column(nullable = false)
    var currentAmount: BigDecimal = BigDecimal.ZERO,

    createdAt: Instant,
    updatedAt: Instant
) : Goal(
    name = name,
    description = description,
    type = GoalType.SAVINGS,
    startDate = startDate,
    endDate = endDate,
    accountId = accountId,
    createdAt = createdAt,
    updatedAt = updatedAt
) {

    /**
     * Processes an account update event.
     * Updates the current amount if the transaction is relevant to this goal.
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
        // Ignore if not for this account or if goal has ended
        if (this.accountId != accountId || hasEnded()) {
            return false
        }

        if (transactionType == "CREDIT") {
            // Add the transaction amount to the current amount
            currentAmount = currentAmount.add(transactionAmount)
            updatedAt = Instant.now(clock)

            // Check if the target has been reached
            if (currentAmount >= targetAmount) {
                updateStatus(GoalStatus.ACHIEVED, clock)
                return true
            }
        }

        // Check if the goal's timeframe has expired
        if (isExpired(clock) && isActive()) {
            // If we've reached the target, the goal is achieved
            if (currentAmount >= targetAmount) {
                updateStatus(GoalStatus.ACHIEVED, clock)
                return true
            } else {
                updateStatus(GoalStatus.FAILED, clock)
                return true
            }
        }

        return false
    }
}