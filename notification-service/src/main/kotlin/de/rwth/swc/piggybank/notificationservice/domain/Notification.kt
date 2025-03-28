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
 * Represents a notification for an account.
 *
 * @property id The unique identifier of the notification
 * @property accountId The identifier of the account related to the notification
 * @property eventType The type of event that triggered the notification
 * @property message The notification message
 * @property read Whether the notification has been read
 * @property createdAt The timestamp when the notification was created
 */
@Entity
@Table(name = "notifications")
data class Notification(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "account_id", nullable = false)
    val accountId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: NotificationEventType,

    @Column(name = "message", nullable = false, length = 1000)
    val message: String,

    @Column(name = "read", nullable = false)
    val read: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Marks this notification as read.
     *
     * @return A new Notification with read set to true
     */
    fun markAsRead(): Notification {
        return copy(read = true)
    }

    companion object {
        /**
         * Creates a new notification.
         *
         * @param accountId The identifier of the account
         * @param eventType The type of event
         * @param message The notification message
         * @return The created notification
         */
        fun create(accountId: String, eventType: NotificationEventType, message: String): Notification {
            return Notification(
                accountId = accountId,
                eventType = eventType,
                message = message
            )
        }
    }
}
