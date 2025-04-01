package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.CreateSavingsGoalRequest
import de.rwth.swc.piggybank.goalservice.dto.CreateSpendingLimitGoalRequest
import de.rwth.swc.piggybank.goalservice.dto.SavingsGoalResponse
import de.rwth.swc.piggybank.goalservice.dto.SpendingLimitGoalResponse
import java.util.UUID

/**
 * Service interface for goal operations.
 */
interface GoalService {
    /**
     * Creates a new spending limit goal.
     *
     * @param request The request containing the goal details
     * @return The created goal
     */
    fun createSpendingLimitGoal(request: CreateSpendingLimitGoalRequest): SpendingLimitGoalResponse

    /**
     * Creates a new savings goal.
     *
     * @param request The request containing the goal details
     * @return The created goal
     */
    fun createSavingsGoal(request: CreateSavingsGoalRequest): SavingsGoalResponse

    /**
     * Gets a goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or null if not found
     */
    fun getGoalById(id: UUID): Goal?

    /**
     * Gets a spending limit goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or null if not found
     */
    fun getSpendingLimitGoalById(id: UUID): SpendingLimitGoal?

    /**
     * Gets a savings goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or null if not found
     */
    fun getSavingsGoalById(id: UUID): SavingsGoal?

    /**
     * Gets all goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of goals for the account
     */
    fun getGoalsByAccountId(accountId: String): List<Goal>

    /**
     * Gets all spending limit goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of spending limit goals for the account
     */
    fun getSpendingLimitGoalsByAccountId(accountId: String): List<SpendingLimitGoal>

    /**
     * Gets all savings goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of savings goals for the account
     */
    fun getSavingsGoalsByAccountId(accountId: String): List<SavingsGoal>

    /**
     * Gets all goals with a specific status.
     *
     * @param status The status of the goals
     * @return A list of goals with the status
     */
    fun getGoalsByStatus(status: GoalStatus): List<Goal>

    /**
     * Gets all spending limit goals for a specific category.
     *
     * @param category The category of the goals
     * @return A list of spending limit goals for the category
     */
    fun getSpendingLimitGoalsByCategory(category: String): List<SpendingLimitGoal>

    /**
     * Updates the status of a goal.
     *
     * @param id The ID of the goal
     * @param status The new status of the goal
     * @return The updated goal, or null if not found
     */
    fun updateGoalStatus(id: UUID, status: GoalStatus): Goal?

    /**
     * Deletes a goal.
     *
     * @param id The ID of the goal
     * @return true if the goal was deleted, false otherwise
     */
    fun deleteGoal(id: UUID): Boolean
}