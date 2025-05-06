package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.CreateSavingsGoalRequest
import de.rwth.swc.piggybank.goalservice.dto.CreateSpendingLimitGoalRequest
import de.rwth.swc.piggybank.goalservice.dto.SavingsGoalResponse
import de.rwth.swc.piggybank.goalservice.dto.SpendingLimitGoalResponse
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import de.rwth.swc.piggybank.goalservice.repository.SavingsGoalRepository
import de.rwth.swc.piggybank.goalservice.repository.SpendingLimitGoalRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

/**
 * Implementation of the GoalService interface.
 */
@Service
class GoalServiceImpl(
    private val goalRepository: GoalRepository,
    private val spendingLimitGoalRepository: SpendingLimitGoalRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val rabbitMQService: RabbitMQService,
    private val clock: Clock
) : GoalService {
    private val logger = LoggerFactory.getLogger(GoalServiceImpl::class.java)

    /**
     * Creates a new spending limit goal.
     *
     * @param request The request containing the goal details
     * @return The created goal
     */
    @Transactional
    override fun createSpendingLimitGoal(request: CreateSpendingLimitGoalRequest): SpendingLimitGoalResponse {
        logger.info("Creating spending limit goal: {}", request)
        val goal = request.toDomain(clock)
        val savedGoal = spendingLimitGoalRepository.save(goal)
        logger.info("Spending limit goal created: {}", savedGoal)
        return SpendingLimitGoalResponse.fromDomain(savedGoal)
    }

    /**
     * Creates a new savings goal.
     *
     * @param request The request containing the goal details
     * @return The created goal
     */
    @Transactional
    override fun createSavingsGoal(request: CreateSavingsGoalRequest): SavingsGoalResponse {
        logger.info("Creating savings goal: {}", request)
        val goal = request.toDomain(clock)
        val savedGoal = savingsGoalRepository.save(goal)
        logger.info("Savings goal created: {}", savedGoal)
        return SavingsGoalResponse.fromDomain(savedGoal)
    }

    /**
     * Gets a goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or null if not found
     */
    @Transactional(readOnly = true)
    override fun getGoalById(id: UUID): Goal? {
        logger.info("Getting goal by ID: {}", id)
        return goalRepository.findById(id).orElse(null)
    }

    /**
     * Gets a spending limit goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or null if not found
     */
    @Transactional(readOnly = true)
    override fun getSpendingLimitGoalById(id: UUID): SpendingLimitGoal? {
        logger.info("Getting spending limit goal by ID: {}", id)
        return spendingLimitGoalRepository.findById(id).orElse(null)
    }

    /**
     * Gets a savings goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or null if not found
     */
    @Transactional(readOnly = true)
    override fun getSavingsGoalById(id: UUID): SavingsGoal? {
        logger.info("Getting savings goal by ID: {}", id)
        return savingsGoalRepository.findById(id).orElse(null)
    }

    /**
     * Gets all goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of goals for the account
     */
    @Transactional(readOnly = true)
    override fun getGoalsByAccountId(accountId: String): List<Goal> {
        logger.info("Getting goals for account: {}", accountId)
        return goalRepository.findByAccountId(accountId)
    }

    /**
     * Gets all spending limit goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of spending limit goals for the account
     */
    @Transactional(readOnly = true)
    override fun getSpendingLimitGoalsByAccountId(accountId: String): List<SpendingLimitGoal> {
        logger.info("Getting spending limit goals for account: {}", accountId)
        return spendingLimitGoalRepository.findByAccountId(accountId)
    }

    /**
     * Gets all savings goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of savings goals for the account
     */
    @Transactional(readOnly = true)
    override fun getSavingsGoalsByAccountId(accountId: String): List<SavingsGoal> {
        logger.info("Getting savings goals for account: {}", accountId)
        return savingsGoalRepository.findByAccountId(accountId)
    }

    /**
     * Gets all goals with a specific status.
     *
     * @param status The status of the goals
     * @return A list of goals with the status
     */
    @Transactional(readOnly = true)
    override fun getGoalsByStatus(status: GoalStatus): List<Goal> {
        logger.info("Getting goals with status: {}", status)
        return goalRepository.findByStatus(status)
    }

    /**
     * Gets all spending limit goals for a specific category.
     *
     * @param category The category of the goals
     * @return A list of spending limit goals for the category
     */
    @Transactional(readOnly = true)
    override fun getSpendingLimitGoalsByCategory(category: String): List<SpendingLimitGoal> {
        logger.info("Getting spending limit goals for category: {}", category)
        return spendingLimitGoalRepository.findByCategory(category)
    }

    /**
     * Updates the status of a goal.
     *
     * @param id The ID of the goal
     * @param status The new status of the goal
     * @return The updated goal, or null if not found
     */
    @Transactional
    override fun updateGoalStatus(id: UUID, status: GoalStatus): Goal? {
        logger.info("Updating goal status: {} -> {}", id, status)
        val goal = goalRepository.findById(id).orElse(null) ?: return null
        goal.updateStatus(status, clock)
        val savedGoal = goalRepository.save(goal)

        // Send event to RabbitMQ
        try {
            rabbitMQService.sendGoalStatusEvent(savedGoal)
            logger.info("Goal status event sent for goal: {}", savedGoal.id)
        } catch (e: Exception) {
            logger.error("Failed to send goal status event", e)
            // Don't rethrow the exception to avoid affecting the main operation
        }

        return savedGoal
    }

    /**
     * Deletes a goal.
     *
     * @param id The ID of the goal
     * @return true if the goal was deleted, false otherwise
     */
    @Transactional
    override fun deleteGoal(id: UUID): Boolean {
        logger.info("Deleting goal: {}", id)
        if (!goalRepository.existsById(id)) {
            return false
        }
        goalRepository.deleteById(id)
        return true
    }
}
