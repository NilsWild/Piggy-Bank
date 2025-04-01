package de.rwth.swc.piggybank.goalservice.dto

import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.GoalType
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import java.math.BigDecimal
import java.time.LocalDateTime
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
    abstract val startDate: LocalDateTime
    abstract val endDate: LocalDateTime
    abstract val accountId: String
    abstract val createdAt: LocalDateTime
    abstract val updatedAt: LocalDateTime
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
    override val startDate: LocalDateTime,
    override val endDate: LocalDateTime,
    override val accountId: String,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
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
    override val startDate: LocalDateTime,
    override val endDate: LocalDateTime,
    override val accountId: String,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
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
    abstract val startDate: LocalDateTime
    abstract val endDate: LocalDateTime
    abstract val accountId: String
}

/**
 * Request DTO for creating spending limit goals.
 */
data class CreateSpendingLimitGoalRequest(
    override val name: String,
    override val description: String? = null,
    override val startDate: LocalDateTime,
    override val endDate: LocalDateTime,
    override val accountId: String,
    val limit: BigDecimal,
    val currencyCode: String,
    val category: String
) : CreateGoalRequest() {
    /**
     * Converts this request to a SpendingLimitGoal.
     *
     * @return The SpendingLimitGoal
     */
    fun toDomain(): SpendingLimitGoal {
        return SpendingLimitGoal(
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            accountId = accountId,
            limit = limit,
            currencyCode = currencyCode,
            category = category
        )
    }
}

/**
 * Request DTO for creating savings goals.
 */
data class CreateSavingsGoalRequest(
    override val name: String,
    override val description: String? = null,
    override val startDate: LocalDateTime,
    override val endDate: LocalDateTime,
    override val accountId: String,
    val targetAmount: BigDecimal,
    val currencyCode: String
) : CreateGoalRequest() {
    /**
     * Converts this request to a SavingsGoal.
     *
     * @return The SavingsGoal
     */
    fun toDomain(): SavingsGoal {
        return SavingsGoal(
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            accountId = accountId,
            targetAmount = targetAmount,
            currencyCode = currencyCode
        )
    }
}