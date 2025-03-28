package de.rwth.swc.piggybank.notificationservice.service

import de.rwth.swc.piggybank.notificationservice.domain.Notification
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
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(RabbitMQService::class.java)

    companion object {
        const val NOTIFICATION_EXCHANGE_NAME = "piggybank.notifications"
        const val NOTIFICATION_ROUTING_KEY = "notification.created"
    }

    /**
     * Listens for account updated events and processes them.
     *
     * @param event The account updated event
     */
    @RabbitListener(queues = ["piggybank.accounts.updated.notifications.queue"])
    fun handleAccountUpdatedEvent(event: Map<String, Any>) {
        logger.info("Received account updated event: {}", event)
        try {
            // Extract event data
            val eventType = event["eventType"] as String
            if (eventType != "ACCOUNT_UPDATED") {
                logger.warn("Ignoring non-ACCOUNT_UPDATED event: {}", eventType)
                return
            }

            val accountId = event["accountId"] as String
            val transactionType = event["transactionType"] as String
            val transactionAmount = event["transactionAmount"] as Map<String, Any>
            val value = transactionAmount["value"] as Number
            val currencyCode = transactionAmount["currencyCode"] as String
            val purpose = event["transactionPurpose"] as String
            val sourceAccount = event["sourceAccount"] as? String
            val destinationAccount = event["destinationAccount"] as? String

            // Process the event to generate notifications
            notificationService.processAccountUpdatedEvent(
                accountId = accountId,
                transactionType = transactionType,
                amount = value.toDouble(),
                currencyCode = currencyCode,
                purpose = purpose,
                sourceAccount = sourceAccount,
                destinationAccount = destinationAccount
            )
        } catch (e: Exception) {
            logger.error("Failed to process account updated event", e)
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
            val event = mapOf(
                "id" to notification.id.toString(),
                "accountId" to notification.accountId,
                "eventType" to notification.eventType.name,
                "message" to notification.message,
                "read" to notification.read,
                "createdAt" to notification.createdAt.toString()
            )

            // Send to RabbitMQ
            rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE_NAME, NOTIFICATION_ROUTING_KEY, event)
            logger.info("Notification sent to RabbitMQ successfully")
        } catch (e: Exception) {
            logger.error("Failed to send notification", e)
            throw e
        }
    }
}
