package de.rwth.swc.piggybank.notificationservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountUpdatedEvent
import de.rwth.swc.piggybank.goalservice.dto.GoalAchievedEvent
import de.rwth.swc.piggybank.goalservice.dto.GoalFailedEvent
import de.rwth.swc.piggybank.goalservice.dto.GoalUpdatedEvent
import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.dto.NotificationEventDto
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

/**
 * Service for handling RabbitMQ messages.
 */
@Service
class RabbitMQService(
    private val rabbitTemplate: RabbitTemplate,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(RabbitMQService::class.java)

    companion object {
        const val NOTIFICATION_EXCHANGE_NAME = "piggybank.notifications"
        const val NOTIFICATION_ROUTING_KEY = "notification.created"
        const val GOAL_EXCHANGE_NAME = "piggybank.goals"
        const val GOAL_UPDATED_ROUTING_KEY = "goal.updated"
        const val GOAL_ACHIEVED_ROUTING_KEY = "goal.achieved"
        const val GOAL_FAILED_ROUTING_KEY = "goal.failed"
    }

    /**
     * Listens for account updated events and processes them.
     *
     * @param eventMap The account updated event as a Map
     */
    @RabbitListener(queues = ["piggybank.accounts.updated.notifications.queue"])
    fun handleAccountUpdatedEvent(event: AccountUpdatedEvent) {
        logger.info("Received account updated event: {}", event)




            // Extract values from the event
            val value = event.transactionAmount.value.toDouble()

            // Process the event to generate notifications
            notificationService.processAccountUpdatedEvent(
                accountId = event.accountId,
                transactionType = event.transactionType,
                amount = value.toDouble(),
                currencyCode = event.transactionAmount.currencyCode,
                purpose = event.transactionPurpose
            )
    }

    /**
     * Listens for goal updated events and processes them.
     *
     * @param event The goal updated event
     */
    @RabbitListener(queues = ["piggybank.goals.updated.notifications.queue"])
    fun handleGoalUpdatedEvent(event: GoalUpdatedEvent) {
        logger.info("Received goal updated event: {}", event)
        try {
            // Process the event to generate notifications
            notificationService.processGoalUpdatedEvent(
                accountId = event.accountId,
                goalId = event.goalId,
                goalName = event.goalName,
                goalType = event.goalType,
                progress = event.progress,
                target = event.target,
                currencyCode = event.currencyCode
            )
        } catch (e: Exception) {
            logger.error("Failed to process goal updated event", e)
        }
    }

    /**
     * Listens for goal achieved events and processes them.
     *
     * @param event The goal achieved event
     */
    @RabbitListener(queues = ["piggybank.goals.achieved.notifications.queue"])
    fun handleGoalAchievedEvent(event: GoalAchievedEvent) {
        logger.info("Received goal achieved event: {}", event)
        try {
            // Process the event to generate notifications
            notificationService.processGoalAchievedEvent(
                accountId = event.accountId,
                goalId = event.goalId,
                goalName = event.goalName
            )
        } catch (e: Exception) {
            logger.error("Failed to process goal achieved event", e)
        }
    }

    /**
     * Listens for goal failed events and processes them.
     *
     * @param event The goal failed event
     */
    @RabbitListener(queues = ["piggybank.goals.failed.notifications.queue"])
    fun handleGoalFailedEvent(event: GoalFailedEvent) {
        logger.info("Received goal failed event: {}", event)
        try {
            // Process the event to generate notifications
            notificationService.processGoalFailedEvent(
                accountId = event.accountId,
                goalId = event.goalId,
                goalName = event.goalName
            )
        } catch (e: Exception) {
            logger.error("Failed to process goal failed event", e)
        }
    }

    /**
     * Sends a notification to RabbitMQ.
     *
     * @param notification The notification to send
     */
    fun sendNotification(notification: Notification) {
        logger.info("Sending notification to RabbitMQ: {}", notification)
        try {
            // Create event map for RabbitMQ
            val event = NotificationEventDto.fromDomain(notification)

            // Send to RabbitMQ
            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE_NAME, NOTIFICATION_ROUTING_KEY, event)
            logger.info("Notification sent to RabbitMQ successfully")
        } catch (e: Exception) {
            logger.error("Failed to send notification", e)
            throw e
        }
    }
}
