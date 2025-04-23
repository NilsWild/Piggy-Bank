package de.rwth.swc.piggybank.goalservice.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Enum representing the status of a goal.
 */
enum class GoalStatus {
    /**
     * The goal is active and being tracked.
     */
    ACTIVE,

    /**
     * The goal has been achieved.
     */
    ACHIEVED,

    /**
     * The goal has failed (timeframe ended before the goal was achieved).
     */
    FAILED,

    /**
     * The goal has been cancelled by the user.
     */
    CANCELLED
}

/**
 * Enum representing the type of a goal.
 */
enum class GoalType {
    /**
     * A goal to limit spending on a specific category.
     */
    SPENDING_LIMIT,

    /**
     * A goal to save a certain amount of money.
     */
    SAVINGS
}

/**
 * Base class for all goals.
 * This is an abstract class that defines common properties and methods for all goal types.
 *
 * @property id The unique identifier of the goal
 * @property name The name of the goal
 * @property description The description of the goal
 * @property type The type of the goal
 * @property status The status of the goal
 * @property startDate The date when the goal starts
 * @property endDate The date when the goal ends
 * @property accountId The ID of the account associated with the goal
 * @property createdAt The date when the goal was created
 * @property updatedAt The date when the goal was last updated
 */
@Entity
@Table(name = "goals")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Goal(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String,

    @Column(nullable = true, length = 1000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: GoalType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GoalStatus = GoalStatus.ACTIVE,

    @Column(nullable = false)
    val startDate: LocalDateTime,

    @Column(nullable = false)
    val endDate: LocalDateTime,

    @Column(nullable = false)
    val accountId: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Updates the status of the goal.
     *
     * @param newStatus The new status of the goal
     */
    fun updateStatus(newStatus: GoalStatus) {
        status = newStatus
        updatedAt = LocalDateTime.now()
    }

    /**
     * Checks if the goal is active.
     *
     * @return true if the goal is active, false otherwise
     */
    fun isActive(): Boolean {
        return status == GoalStatus.ACTIVE
    }

    /**
     * Checks if the goal has ended (achieved, failed, or cancelled).
     *
     * @return true if the goal has ended, false otherwise
     */
    fun hasEnded(): Boolean {
        return status == GoalStatus.ACHIEVED || status == GoalStatus.FAILED || status == GoalStatus.CANCELLED
    }

    /**
     * Checks if the goal's timeframe has expired.
     *
     * @return true if the goal's timeframe has expired, false otherwise
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(endDate)
    }

    /**
     * Processes an account update event.
     * This method should be implemented by subclasses to update the goal's progress based on account updates.
     *
     * @param accountId The ID of the account
     * @param transactionAmount The amount of the transaction
     * @param transactionType The type of the transaction
     * @param transactionPurpose The purpose of the transaction
     * @param classifications The classifications of the transaction
     * @return true if the goal's status changed, false otherwise
     */
    abstract fun processAccountUpdate(
        accountId: String,
        transactionAmount: BigDecimal,
        transactionType: String,
        transactionPurpose: String,
        classifications: List<String>
    ): Boolean
}
