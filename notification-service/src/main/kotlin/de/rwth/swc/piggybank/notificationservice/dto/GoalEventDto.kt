package de.rwth.swc.piggybank.notificationservice.dto

import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Base class for goal events received from the goal-service.
 */
sealed class GoalEvent : Serializable {
    abstract val eventType: String
    abstract val goalId: UUID
    abstract val goalName: String
    abstract val goalType: String
    abstract val goalStatus: String
    abstract val accountId: String
    abstract val timestamp: LocalDateTime
}

/**
 * Event received when a goal is updated.
 */
data class GoalUpdatedEvent(
    override val eventType: String,
    override val goalId: UUID,
    override val goalName: String,
    override val goalType: String,
    override val goalStatus: String,
    override val accountId: String,
    override val timestamp: LocalDateTime,
    val progress: BigDecimal,
    val target: BigDecimal,
    val currencyCode: String
) : GoalEvent()

/**
 * Event received when a goal is achieved.
 */
data class GoalAchievedEvent(
    override val eventType: String,
    override val goalId: UUID,
    override val goalName: String,
    override val goalType: String,
    override val goalStatus: String,
    override val accountId: String,
    override val timestamp: LocalDateTime
) : GoalEvent()

/**
 * Event received when a goal fails.
 */
data class GoalFailedEvent(
    override val eventType: String,
    override val goalId: UUID,
    override val goalName: String,
    override val goalType: String,
    override val goalStatus: String,
    override val accountId: String,
    override val timestamp: LocalDateTime
) : GoalEvent()