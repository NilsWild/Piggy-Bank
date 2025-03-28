package de.rwth.swc.piggybank.notificationservice.service

import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.domain.NotificationSubscription
import de.rwth.swc.piggybank.notificationservice.repository.NotificationRepository
import de.rwth.swc.piggybank.notificationservice.repository.NotificationSubscriptionRepository
import org.springframework.context.annotation.Lazy
import de.rwth.swc.piggybank.notificationservice.service.RabbitMQService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service for managing notifications and notification subscriptions.
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val subscriptionRepository: NotificationSubscriptionRepository,
    @Lazy private val rabbitMQService: RabbitMQService
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * Processes an account updated event and generates notifications for subscribed accounts.
     *
     * @param accountId The identifier of the account
     * @param transactionType The type of the transaction (CREDIT or DEBIT)
     * @param amount The amount of the transaction
     * @param currencyCode The currency code of the amount
     * @param purpose The purpose of the transaction
     * @param sourceAccount The source account of the transaction (for CREDIT transactions)
     * @param destinationAccount The destination account of the transaction (for DEBIT transactions)
     */
    @Transactional
    fun processAccountUpdatedEvent(
        accountId: String,
        transactionType: String,
        amount: Double,
        currencyCode: String,
        purpose: String,
        sourceAccount: String?,
        destinationAccount: String?
    ) {
        logger.info("Processing account updated event for account: {}", accountId)

        // Find all active subscriptions for this account and event type
        val subscriptions = subscriptionRepository.findByAccountIdAndEventTypeAndActiveTrue(
            accountId,
            NotificationEventType.BALANCE_UPDATE
        )

        if (subscriptions.isEmpty()) {
            logger.info("No active subscriptions found for account: {}", accountId)
            return
        }

        // Generate notification message based on transaction type
        val message = when (transactionType) {
            "CREDIT" -> "You just received $amount $currencyCode" + 
                        (if (sourceAccount != null) " from $sourceAccount" else "") +
                        (if (purpose.isNotBlank()) " for: $purpose" else "")
            "DEBIT" -> "You just sent $amount $currencyCode" + 
                       (if (destinationAccount != null) " to $destinationAccount" else "") +
                       (if (purpose.isNotBlank()) " for: $purpose" else "")
            else -> "Your account balance has been updated by $amount $currencyCode"
        }

        // Create and save a notification
        val notification = Notification.create(
            accountId = accountId,
            eventType = NotificationEventType.BALANCE_UPDATE,
            message = message
        )
        val savedNotification = notificationRepository.save(notification)

        // Send the notification to RabbitMQ
        rabbitMQService.sendNotification(savedNotification)

        logger.info("Created and sent notification for account: {}", accountId)
    }

    /**
     * Creates a new notification subscription.
     *
     * @param accountId The identifier of the account
     * @param eventType The type of event
     * @return The created subscription
     */
    @Transactional
    fun createSubscription(accountId: String, eventType: NotificationEventType): NotificationSubscription {
        logger.info("Creating subscription for account: {}, event type: {}", accountId, eventType)

        // Check if a subscription already exists
        val existingSubscription = subscriptionRepository.findFirstByAccountIdAndEventTypeAndActiveTrue(
            accountId,
            eventType
        )

        if (existingSubscription != null) {
            logger.info("Subscription already exists: {}", existingSubscription)
            return existingSubscription
        }

        // Create and save a new subscription
        val subscription = NotificationSubscription.create(accountId, eventType)
        return subscriptionRepository.save(subscription)
    }

    /**
     * Deactivates a notification subscription.
     *
     * @param subscriptionId The identifier of the subscription
     * @return true if the subscription was deactivated, false if it was not found
     */
    @Transactional
    fun deactivateSubscription(subscriptionId: UUID): Boolean {
        logger.info("Deactivating subscription with ID: {}", subscriptionId)

        val subscription = subscriptionRepository.findById(subscriptionId).orElse(null) ?: return false
        val deactivatedSubscription = subscription.deactivate()
        subscriptionRepository.save(deactivatedSubscription)
        return true
    }

    /**
     * Gets all active subscriptions.
     *
     * @return A list of active subscriptions
     */
    fun getAllSubscriptions(): List<NotificationSubscription> {
        logger.info("Getting all active subscriptions")
        return subscriptionRepository.findByActiveTrue()
    }

    /**
     * Gets all active subscriptions for an account.
     *
     * @param accountId The identifier of the account
     * @return A list of active subscriptions
     */
    fun getAccountSubscriptions(accountId: String): List<NotificationSubscription> {
        logger.info("Getting subscriptions for account: {}", accountId)
        return subscriptionRepository.findByAccountIdAndActiveTrue(accountId)
    }

    /**
     * Gets all notifications.
     *
     * @param pageable The pagination information
     * @return A page of notifications
     */
    fun getAllNotifications(pageable: Pageable): Page<Notification> {
        logger.info("Getting all notifications")
        return notificationRepository.findAll(pageable)
    }

    /**
     * Gets all notifications for a specific account.
     *
     * @param accountId The identifier of the account
     * @param pageable The pagination information
     * @return A page of notifications
     */
    fun getAccountNotifications(accountId: String, pageable: Pageable): Page<Notification> {
        logger.info("Getting notifications for account: {}", accountId)
        return notificationRepository.findByAccountId(accountId, pageable)
    }

    /**
     * Gets all unread notifications.
     *
     * @param pageable The pagination information
     * @return A page of unread notifications
     */
    fun getUnreadNotifications(pageable: Pageable): Page<Notification> {
        logger.info("Getting unread notifications")
        return notificationRepository.findByReadFalse(pageable)
    }

    /**
     * Gets all unread notifications for a specific account.
     *
     * @param accountId The identifier of the account
     * @param pageable The pagination information
     * @return A page of unread notifications
     */
    fun getUnreadNotificationsForAccount(accountId: String, pageable: Pageable): Page<Notification> {
        logger.info("Getting unread notifications for account: {}", accountId)
        return notificationRepository.findByAccountIdAndReadFalse(accountId, pageable)
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId The identifier of the notification
     * @return true if the notification was marked as read, false if it was not found
     */
    @Transactional
    fun markNotificationAsRead(notificationId: UUID): Boolean {
        logger.info("Marking notification as read: {}", notificationId)

        val notification = notificationRepository.findById(notificationId).orElse(null) ?: return false
        val readNotification = notification.markAsRead()
        val savedNotification = notificationRepository.save(readNotification)

        // Send the updated notification to RabbitMQ
        rabbitMQService.sendNotification(savedNotification)

        return true
    }

    /**
     * Counts the number of unread notifications.
     *
     * @return The number of unread notifications
     */
    fun countUnreadNotifications(): Long {
        logger.info("Counting unread notifications")
        return notificationRepository.countByReadFalse()
    }
}
