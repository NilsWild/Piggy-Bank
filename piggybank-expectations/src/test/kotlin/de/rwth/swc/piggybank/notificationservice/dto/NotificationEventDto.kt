package de.rwth.swc.piggybank.notificationservice.dto

import java.time.Instant

/**
 * Data Transfer Object for notification events sent to RabbitMQ.
 *
 * @property id The unique identifier of the notification
 * @property accountId The identifier of the account related to the notification
 * @property eventType The type of event that triggered the notification
 * @property message The notification message
 * @property read Whether the notification has been read
 * @property createdAt The timestamp when the notification was created
 */
data class NotificationEventDto(
    val id: String,
    val accountId: String,
    val eventType: String,
    val message: String,
    val read: Boolean,
    val createdAt: Instant
) {
    companion object {
    }
}