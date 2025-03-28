package de.rwth.swc.piggybank.notificationservice.repository

import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.domain.NotificationSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for managing notification subscriptions.
 */
@Repository
interface NotificationSubscriptionRepository : JpaRepository<NotificationSubscription, UUID> {
    /**
     * Finds all active subscriptions.
     *
     * @return A list of active subscriptions
     */
    fun findByActiveTrue(): List<NotificationSubscription>

    /**
     * Finds all active subscriptions for an account.
     *
     * @param accountId The identifier of the account
     * @return A list of active subscriptions
     */
    fun findByAccountIdAndActiveTrue(accountId: String): List<NotificationSubscription>

    /**
     * Finds all active subscriptions for a specific event type.
     *
     * @param eventType The type of event
     * @return A list of active subscriptions
     */
    fun findByEventTypeAndActiveTrue(eventType: NotificationEventType): List<NotificationSubscription>

    /**
     * Finds an active subscription for a specific account and event type.
     *
     * @param accountId The identifier of the account
     * @param eventType The type of event
     * @return The subscription, or null if not found
     */
    fun findByAccountIdAndEventTypeAndActiveTrue(
        accountId: String,
        eventType: NotificationEventType
    ): List<NotificationSubscription>

    /**
     * Finds a single active subscription for a specific account and event type.
     *
     * @param accountId The identifier of the account
     * @param eventType The type of event
     * @return The subscription, or null if not found
     */
    fun findFirstByAccountIdAndEventTypeAndActiveTrue(
        accountId: String,
        eventType: NotificationEventType
    ): NotificationSubscription?
}
