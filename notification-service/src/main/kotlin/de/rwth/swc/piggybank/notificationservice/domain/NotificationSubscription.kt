package de.rwth.swc.piggybank.notificationservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Represents a subscription to notifications for a specific account and event type.
 *
 * @property id The unique identifier of the subscription
 * @property accountId The identifier of the account for which notifications are subscribed
 * @property eventType The type of event for which notifications are subscribed
 * @property active Whether the subscription is active
 * @property createdAt The timestamp when the subscription was created
 */
@Entity
@Table(name = "notification_subscriptions")
data class NotificationSubscription(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "account_id", nullable = false)
    val accountId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: NotificationEventType,

    @Column(name = "active", nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Deactivates this subscription.
     *
     * @return A new NotificationSubscription with active set to false
     */
    fun deactivate(): NotificationSubscription {
        return copy(active = false)
    }

    /**
     * Activates this subscription.
     *
     * @return A new NotificationSubscription with active set to true
     */
    fun activate(): NotificationSubscription {
        return copy(active = true)
    }

    companion object {
        /**
         * Creates a new active subscription.
         *
         * @param accountId The identifier of the account
         * @param eventType The type of event
         * @return The created subscription
         */
        fun create(accountId: String, eventType: NotificationEventType): NotificationSubscription {
            return NotificationSubscription(
                accountId = accountId,
                eventType = eventType
            )
        }
    }
}
