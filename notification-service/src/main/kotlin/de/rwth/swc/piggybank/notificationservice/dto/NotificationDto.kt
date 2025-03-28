package de.rwth.swc.piggybank.notificationservice.dto

import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object for returning notification information.
 *
 * @property id The unique identifier of the notification
 * @property accountId The identifier of the account
 * @property eventType The type of event
 * @property message The notification message
 * @property read Whether the notification has been read
 * @property createdAt The timestamp when the notification was created
 */
data class NotificationResponse(
    val id: UUID,
    val accountId: String,
    val eventType: NotificationEventType,
    val message: String,
    val read: Boolean,
    val createdAt: Instant
) {
    companion object {
        /**
         * Creates a NotificationResponse from a domain Notification object.
         *
         * @param notification The domain Notification object
         * @return The NotificationResponse
         */
        fun fromDomain(notification: Notification): NotificationResponse {
            return NotificationResponse(
                id = notification.id,
                accountId = notification.accountId,
                eventType = notification.eventType,
                message = notification.message,
                read = notification.read,
                createdAt = notification.createdAt
            )
        }
    }
}

/**
 * Data Transfer Object for pagination information.
 *
 * @property content The content of the page
 * @property totalElements The total number of elements
 * @property totalPages The total number of pages
 * @property size The size of the page
 * @property number The page number
 */
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
) {
    companion object {
        /**
         * Creates a PageResponse from a Spring Data Page.
         *
         * @param page The Spring Data Page
         * @param mapper A function to map the page content to the desired type
         * @return The PageResponse
         */
        fun <T, R> fromPage(page: org.springframework.data.domain.Page<T>, mapper: (T) -> R): PageResponse<R> {
            return PageResponse(
                content = page.content.map(mapper),
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                size = page.size,
                number = page.number
            )
        }
    }
}

/**
 * Data Transfer Object for unread notification count.
 *
 * @property count The number of unread notifications
 */
data class UnreadNotificationCount(
    val count: Long
)