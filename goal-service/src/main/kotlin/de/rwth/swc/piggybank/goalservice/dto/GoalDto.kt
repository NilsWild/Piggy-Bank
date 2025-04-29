package de.rwth.swc.piggybank.goalservice.dto

import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.GoalType
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Base class for goal response DTOs.
 * Contains common properties for all goal types.
 */
sealed class GoalResponse {
    abstract val id: UUID
    abstract val name: String
    abstract val description: String?
    abstract val type: GoalType
    abstract val status: GoalStatus
    abstract val startDate: Instant
    abstract val endDate: Instant
    abstract val accountId: String
    abstract val createdAt: Instant
    abstract val updatedAt: Instant
}

/**
 * Response DTO for spending limit goals.
 */
data class SpendingLimitGoalResponse(
    override val id: UUID,
    override val name: String,
    override val description: String?,
    override val type: GoalType,
    override val status: GoalStatus,
    override val startDate: Instant,
    override val endDate: Instant,
    override val accountId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val limit: BigDecimal,
    val currencyCode: String,
    val category: String,
    val currentSpending: BigDecimal
) : GoalResponse() {
    companion object {
        /**
         * Creates a SpendingLimitGoalResponse from a SpendingLimitGoal.
         *
         * @param goal The SpendingLimitGoal to convert
         * @return The SpendingLimitGoalResponse
         */
        fun fromDomain(goal: SpendingLimitGoal): SpendingLimitGoalResponse {
            return SpendingLimitGoalResponse(
                id = goal.id,
                name = goal.name,
                description = goal.description,
                type = goal.type,
                status = goal.status,
                startDate = goal.startDate,
                endDate = goal.endDate,
                accountId = goal.accountId,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt,
                limit = goal.limit,
                currencyCode = goal.currencyCode,
                category = goal.category,
                currentSpending = goal.currentSpending
            )
        }
    }
}

/**
 * Response DTO for savings goals.
 */
data class SavingsGoalResponse(
    override val id: UUID,
    override val name: String,
    override val description: String?,
    override val type: GoalType,
    override val status: GoalStatus,
    override val startDate: Instant,
    override val endDate: Instant,
    override val accountId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val targetAmount: BigDecimal,
    val currencyCode: String,
    val currentAmount: BigDecimal
) : GoalResponse() {
    companion object {
        /**
         * Creates a SavingsGoalResponse from a SavingsGoal.
         *
         * @param goal The SavingsGoal to convert
         * @return The SavingsGoalResponse
         */
        fun fromDomain(goal: SavingsGoal): SavingsGoalResponse {
            return SavingsGoalResponse(
                id = goal.id,
                name = goal.name,
                description = goal.description,
                type = goal.type,
                status = goal.status,
                startDate = goal.startDate,
                endDate = goal.endDate,
                accountId = goal.accountId,
                createdAt = goal.createdAt,
                updatedAt = goal.updatedAt,
                targetAmount = goal.targetAmount,
                currencyCode = goal.currencyCode,
                currentAmount = goal.currentAmount
            )
        }
    }
}

/**
 * Base class for goal creation request DTOs.
 * Contains common properties for all goal types.
 */
sealed class CreateGoalRequest {
    abstract val name: String
    abstract val description: String?
    abstract val startDate: Instant
    abstract val endDate: Instant
    abstract val accountId: String
}

/**
 * Request DTO for creating spending limit goals.
 */
data class CreateSpendingLimitGoalRequest(
    override val name: String,
    override val description: String? = null,
    override val startDate: Instant,
    override val endDate: Instant,
    override val accountId: String,
    val limit: BigDecimal,
    val currencyCode: String,
    val category: String
) : CreateGoalRequest() {
    /**
     * Converts this request to a SpendingLimitGoal.
     *
     * @param clock The clock to use for getting the current time
     * @return The SpendingLimitGoal
     */
    fun toDomain(clock: java.time.Clock): SpendingLimitGoal {
        return SpendingLimitGoal(
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            accountId = accountId,
            limit = limit,
            currencyCode = currencyCode,
            category = category,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )
    }
}

/**
 * Request DTO for creating savings goals.
 */
data class CreateSavingsGoalRequest(
    override val name: String,
    override val description: String? = null,
    override val startDate: Instant,
    override val endDate: Instant,
    override val accountId: String,
    val targetAmount: BigDecimal,
    val currencyCode: String
) : CreateGoalRequest() {
    /**
     * Converts this request to a SavingsGoal.
     *
     * @param clock The clock to use for getting the current time
     * @return The SavingsGoal
     */
    fun toDomain(clock: java.time.Clock): SavingsGoal {
        return SavingsGoal(
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            accountId = accountId,
            targetAmount = targetAmount,
            currencyCode = currencyCode,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock)
        )
    }
}