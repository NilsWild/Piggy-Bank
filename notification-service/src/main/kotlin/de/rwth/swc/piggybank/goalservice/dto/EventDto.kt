package de.rwth.swc.piggybank.goalservice.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * Base class for goal events sent by the goal-service.
 */
sealed class GoalEvent {
    abstract val eventType: String
    abstract val goalId: UUID
    abstract val goalName: String
    abstract val goalType: GoalType
    abstract val goalStatus: GoalStatus
    abstract val accountId: String
    abstract val timestamp: LocalDateTime
}

/**
 * Event sent when a goal is updated.
 */
data class GoalUpdatedEvent(
    override val eventType: String = "GOAL_UPDATED",
    override val goalId: UUID,
    override val goalName: String,
    override val goalType: GoalType,
    override val goalStatus: GoalStatus,
    override val accountId: String,
    override val timestamp: LocalDateTime = LocalDateTime.now(),
    val progress: BigDecimal,
    val target: BigDecimal,
    val currencyCode: String
) : GoalEvent() {
    companion object {

    }
}

/**
 * Event sent when a goal is achieved.
 */
data class GoalAchievedEvent(
    override val eventType: String = "GOAL_ACHIEVED",
    override val goalId: UUID,
    override val goalName: String,
    override val goalType: GoalType,
    override val goalStatus: GoalStatus,
    override val accountId: String,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : GoalEvent() {
    companion object {
    }
}

/**
 * Event sent when a goal fails.
 */
data class GoalFailedEvent(
    override val eventType: String = "GOAL_FAILED",
    override val goalId: UUID,
    override val goalName: String,
    override val goalType: GoalType,
    override val goalStatus: GoalStatus,
    override val accountId: String,
    override val timestamp: LocalDateTime = LocalDateTime.now()
) : GoalEvent() {
    companion object {

    }
}

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