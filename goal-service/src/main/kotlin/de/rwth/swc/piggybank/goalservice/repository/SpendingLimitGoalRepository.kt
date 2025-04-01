package de.rwth.swc.piggybank.goalservice.repository

import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for SpendingLimitGoal entities.
 * Provides methods for CRUD operations on spending limit goals.
 */
@Repository
interface SpendingLimitGoalRepository : JpaRepository<SpendingLimitGoal, UUID> {
    /**
     * Finds all spending limit goals for a specific account.
     *
     * @param accountId The ID of the account
     * @return A list of spending limit goals for the account
     */
    fun findByAccountId(accountId: String): List<SpendingLimitGoal>

    /**
     * Finds all spending limit goals with a specific status.
     *
     * @param status The status of the goals
     * @return A list of spending limit goals with the status
     */
    fun findByStatus(status: GoalStatus): List<SpendingLimitGoal>

    /**
     * Finds all spending limit goals for a specific account with a specific status.
     *
     * @param accountId The ID of the account
     * @param status The status of the goals
     * @return A list of spending limit goals for the account with the status
     */
    fun findByAccountIdAndStatus(accountId: String, status: GoalStatus): List<SpendingLimitGoal>

    /**
     * Finds all spending limit goals for a specific category.
     *
     * @param category The category of the goals
     * @return A list of spending limit goals for the category
     */
    fun findByCategory(category: String): List<SpendingLimitGoal>

    /**
     * Finds all active spending limit goals for a specific account and category.
     *
     * @param accountId The ID of the account
     * @param category The category of the goals
     * @param status The status of the goals (default: ACTIVE)
     * @return A list of active spending limit goals for the account and category
     */
    fun findByAccountIdAndCategoryAndStatus(
        accountId: String,
        category: String,
        status: GoalStatus = GoalStatus.ACTIVE
    ): List<SpendingLimitGoal>
}