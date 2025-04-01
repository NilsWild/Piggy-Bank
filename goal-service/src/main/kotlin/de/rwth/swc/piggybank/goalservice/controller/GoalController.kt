package de.rwth.swc.piggybank.goalservice.controller

import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.CreateSavingsGoalRequest
import de.rwth.swc.piggybank.goalservice.dto.CreateSpendingLimitGoalRequest
import de.rwth.swc.piggybank.goalservice.dto.SavingsGoalResponse
import de.rwth.swc.piggybank.goalservice.dto.SpendingLimitGoalResponse
import de.rwth.swc.piggybank.goalservice.service.GoalService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST controller for goal operations.
 */
@RestController
@RequestMapping("/api/goals")
class GoalController(private val goalService: GoalService) {
    private val logger = LoggerFactory.getLogger(GoalController::class.java)

    /**
     * Creates a new spending limit goal.
     *
     * @param request The request containing the goal details
     * @return The created goal
     */
    @PostMapping("/spending-limit")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSpendingLimitGoal(@RequestBody request: CreateSpendingLimitGoalRequest): SpendingLimitGoalResponse {
        logger.info("REST request to create spending limit goal: {}", request)
        return goalService.createSpendingLimitGoal(request)
    }

    /**
     * Creates a new savings goal.
     *
     * @param request The request containing the goal details
     * @return The created goal
     */
    @PostMapping("/savings")
    @ResponseStatus(HttpStatus.CREATED)
    fun createSavingsGoal(@RequestBody request: CreateSavingsGoalRequest): SavingsGoalResponse {
        logger.info("REST request to create savings goal: {}", request)
        return goalService.createSavingsGoal(request)
    }

    /**
     * Gets a goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or 404 if not found
     */
    @GetMapping("/{id}")
    fun getGoalById(@PathVariable id: UUID): ResponseEntity<Goal> {
        logger.info("REST request to get goal by ID: {}", id)
        val goal = goalService.getGoalById(id)
        return if (goal != null) {
            ResponseEntity.ok(goal)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Gets a spending limit goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or 404 if not found
     */
    @GetMapping("/spending-limit/{id}")
    fun getSpendingLimitGoalById(@PathVariable id: UUID): ResponseEntity<SpendingLimitGoalResponse> {
        logger.info("REST request to get spending limit goal by ID: {}", id)
        val goal = goalService.getSpendingLimitGoalById(id)
        return if (goal != null) {
            ResponseEntity.ok(SpendingLimitGoalResponse.fromDomain(goal))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Gets a savings goal by ID.
     *
     * @param id The ID of the goal
     * @return The goal, or 404 if not found
     */
    @GetMapping("/savings/{id}")
    fun getSavingsGoalById(@PathVariable id: UUID): ResponseEntity<SavingsGoalResponse> {
        logger.info("REST request to get savings goal by ID: {}", id)
        val goal = goalService.getSavingsGoalById(id)
        return if (goal != null) {
            ResponseEntity.ok(SavingsGoalResponse.fromDomain(goal))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Gets all goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of goals for the account
     */
    @GetMapping("/account/{accountId}")
    fun getGoalsByAccountId(@PathVariable accountId: String): List<Goal> {
        logger.info("REST request to get goals for account: {}", accountId)
        return goalService.getGoalsByAccountId(accountId)
    }

    /**
     * Gets all spending limit goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of spending limit goals for the account
     */
    @GetMapping("/spending-limit/account/{accountId}")
    fun getSpendingLimitGoalsByAccountId(@PathVariable accountId: String): List<SpendingLimitGoalResponse> {
        logger.info("REST request to get spending limit goals for account: {}", accountId)
        return goalService.getSpendingLimitGoalsByAccountId(accountId)
            .map { SpendingLimitGoalResponse.fromDomain(it) }
    }

    /**
     * Gets all savings goals for an account.
     *
     * @param accountId The ID of the account
     * @return A list of savings goals for the account
     */
    @GetMapping("/savings/account/{accountId}")
    fun getSavingsGoalsByAccountId(@PathVariable accountId: String): List<SavingsGoalResponse> {
        logger.info("REST request to get savings goals for account: {}", accountId)
        return goalService.getSavingsGoalsByAccountId(accountId)
            .map { SavingsGoalResponse.fromDomain(it) }
    }

    /**
     * Gets all goals with a specific status.
     *
     * @param status The status of the goals
     * @return A list of goals with the status
     */
    @GetMapping("/status/{status}")
    fun getGoalsByStatus(@PathVariable status: GoalStatus): List<Goal> {
        logger.info("REST request to get goals with status: {}", status)
        return goalService.getGoalsByStatus(status)
    }

    /**
     * Gets all spending limit goals for a specific category.
     *
     * @param category The category of the goals
     * @return A list of spending limit goals for the category
     */
    @GetMapping("/spending-limit/category/{category}")
    fun getSpendingLimitGoalsByCategory(@PathVariable category: String): List<SpendingLimitGoalResponse> {
        logger.info("REST request to get spending limit goals for category: {}", category)
        return goalService.getSpendingLimitGoalsByCategory(category)
            .map { SpendingLimitGoalResponse.fromDomain(it) }
    }

    /**
     * Updates the status of a goal.
     *
     * @param id The ID of the goal
     * @param status The new status of the goal
     * @return The updated goal, or 404 if not found
     */
    @PutMapping("/{id}/status/{status}")
    fun updateGoalStatus(@PathVariable id: UUID, @PathVariable status: GoalStatus): ResponseEntity<Goal> {
        logger.info("REST request to update goal status: {} -> {}", id, status)
        val goal = goalService.updateGoalStatus(id, status)
        return if (goal != null) {
            ResponseEntity.ok(goal)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Deletes a goal.
     *
     * @param id The ID of the goal
     * @return 204 if the goal was deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    fun deleteGoal(@PathVariable id: UUID): ResponseEntity<Void> {
        logger.info("REST request to delete goal: {}", id)
        val deleted = goalService.deleteGoal(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}