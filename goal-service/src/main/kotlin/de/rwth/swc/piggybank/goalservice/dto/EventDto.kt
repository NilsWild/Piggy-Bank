package de.rwth.swc.piggybank.goalservice.dto

import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.GoalType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

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
        /**
         * Creates a GoalUpdatedEvent from a Goal.
         *
         * @param goal The goal that was updated
         * @param progress The current progress of the goal
         * @param target The target of the goal
         * @param currencyCode The currency code of the goal
         * @param clock The clock to use for getting the current time
         * @return The GoalUpdatedEvent
         */
        fun fromDomain(
            goal: Goal,
            progress: BigDecimal,
            target: BigDecimal,
            currencyCode: String,
            clock: java.time.Clock
        ): GoalUpdatedEvent {
            return GoalUpdatedEvent(
                goalId = goal.id,
                goalName = goal.name,
                goalType = goal.type,
                goalStatus = goal.status,
                accountId = goal.accountId,
                timestamp = LocalDateTime.now(clock),
                progress = progress,
                target = target,
                currencyCode = currencyCode
            )
        }
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
        /**
         * Creates a GoalAchievedEvent from a Goal.
         *
         * @param goal The goal that was achieved
         * @param clock The clock to use for getting the current time
         * @return The GoalAchievedEvent
         */
        fun fromDomain(goal: Goal, clock: java.time.Clock): GoalAchievedEvent {
            return GoalAchievedEvent(
                goalId = goal.id,
                goalName = goal.name,
                goalType = goal.type,
                goalStatus = goal.status,
                accountId = goal.accountId,
                timestamp = LocalDateTime.now(clock)
            )
        }
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
        /**
         * Creates a GoalFailedEvent from a Goal.
         *
         * @param goal The goal that failed
         * @param clock The clock to use for getting the current time
         * @return The GoalFailedEvent
         */
        fun fromDomain(goal: Goal, clock: java.time.Clock): GoalFailedEvent {
            return GoalFailedEvent(
                goalId = goal.id,
                goalName = goal.name,
                goalType = goal.type,
                goalStatus = goal.status,
                accountId = goal.accountId,
                timestamp = LocalDateTime.now(clock)
            )
        }
    }
}
