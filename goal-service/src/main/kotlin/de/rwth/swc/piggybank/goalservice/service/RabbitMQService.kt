package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.config.RabbitMQConfig
import de.rwth.swc.piggybank.goalservice.domain.Goal
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SavingsGoal
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.GoalAchievedEvent
import de.rwth.swc.piggybank.goalservice.dto.GoalFailedEvent
import de.rwth.swc.piggybank.goalservice.dto.GoalUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

/**
 * Service for sending goal-related events to RabbitMQ.
 */
@Service
class RabbitMQService(private val rabbitTemplate: RabbitTemplate) {
    private val logger = LoggerFactory.getLogger(RabbitMQService::class.java)

    /**
     * Sends a goal updated event to RabbitMQ.
     *
     * @param goal The goal that was updated
     */
    fun sendGoalUpdatedEvent(goal: Goal) {
        logger.info("Sending goal updated event to RabbitMQ: {}", goal)
        try {
            val event = when (goal) {
                is SpendingLimitGoal -> GoalUpdatedEvent.fromDomain(
                    goal = goal,
                    progress = goal.currentSpending,
                    target = goal.limit,
                    currencyCode = goal.currencyCode
                )
                is SavingsGoal -> GoalUpdatedEvent.fromDomain(
                    goal = goal,
                    progress = goal.currentAmount,
                    target = goal.targetAmount,
                    currencyCode = goal.currencyCode
                )
                else -> throw IllegalArgumentException("Unsupported goal type: ${goal.javaClass.name}")
            }

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.GOAL_EXCHANGE_NAME,
                RabbitMQConfig.GOAL_UPDATED_ROUTING_KEY,
                event
            )
            logger.info("Goal updated event sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send goal updated event to RabbitMQ", e)
            throw e
        }
    }

    /**
     * Sends a goal achieved event to RabbitMQ.
     *
     * @param goal The goal that was achieved
     */
    fun sendGoalAchievedEvent(goal: Goal) {
        logger.info("Sending goal achieved event to RabbitMQ: {}", goal)
        try {
            val event = GoalAchievedEvent.fromDomain(goal)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.GOAL_EXCHANGE_NAME,
                RabbitMQConfig.GOAL_ACHIEVED_ROUTING_KEY,
                event
            )
            logger.info("Goal achieved event sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send goal achieved event to RabbitMQ", e)
            throw e
        }
    }

    /**
     * Sends a goal failed event to RabbitMQ.
     *
     * @param goal The goal that failed
     */
    fun sendGoalFailedEvent(goal: Goal) {
        logger.info("Sending goal failed event to RabbitMQ: {}", goal)
        try {
            val event = GoalFailedEvent.fromDomain(goal)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.GOAL_EXCHANGE_NAME,
                RabbitMQConfig.GOAL_FAILED_ROUTING_KEY,
                event
            )
            logger.info("Goal failed event sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send goal failed event to RabbitMQ", e)
            throw e
        }
    }

    /**
     * Sends the appropriate event based on the goal's status.
     *
     * @param goal The goal to send an event for
     */
    fun sendGoalStatusEvent(goal: Goal) {
        when (goal.status) {
            GoalStatus.ACHIEVED -> sendGoalAchievedEvent(goal)
            GoalStatus.FAILED -> sendGoalFailedEvent(goal)
            else -> sendGoalUpdatedEvent(goal)
        }
    }
}