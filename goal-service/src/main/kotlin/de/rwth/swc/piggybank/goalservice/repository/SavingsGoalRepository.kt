package de.rwth.swc.piggybank.goalservice.repository

import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for SavingsGoal entities.
 * Provides methods for CRUD operations on savings goals.
 */
@Repository
interface SavingsGoalRepository : JpaRepository<SavingsGoal, UUID> {
    /**
     * Finds all savings goals for a specific account.
     *
     * @param accountId The ID of the account
     * @return A list of savings goals for the account
     */
    fun findByAccountId(accountId: String): List<SavingsGoal>

    /**
     * Finds all savings goals with a specific status.
     *
     * @param status The status of the goals
     * @return A list of savings goals with the status
     */
    fun findByStatus(status: GoalStatus): List<SavingsGoal>

    /**
     * Finds all savings goals for a specific account with a specific status.
     *
     * @param accountId The ID of the account
     * @param status The status of the goals
     * @return A list of savings goals for the account with the status
     */
    fun findByAccountIdAndStatus(accountId: String, status: GoalStatus): List<SavingsGoal>
}