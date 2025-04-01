package de.rwth.swc.piggybank.goalservice.repository

import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.GoalType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Goal entities.
 * Provides methods for CRUD operations on goals.
 */
@Repository
interface GoalRepository : JpaRepository<Goal, UUID> {
    /**
     * Finds all goals for a specific account.
     *
     * @param accountId The ID of the account
     * @return A list of goals for the account
     */
    fun findByAccountId(accountId: String): List<Goal>

    /**
     * Finds all goals with a specific status.
     *
     * @param status The status of the goals
     * @return A list of goals with the status
     */
    fun findByStatus(status: GoalStatus): List<Goal>

    /**
     * Finds all goals with a specific type.
     *
     * @param type The type of the goals
     * @return A list of goals with the type
     */
    fun findByType(type: GoalType): List<Goal>

    /**
     * Finds all goals for a specific account with a specific status.
     *
     * @param accountId The ID of the account
     * @param status The status of the goals
     * @return A list of goals for the account with the status
     */
    fun findByAccountIdAndStatus(accountId: String, status: GoalStatus): List<Goal>
}