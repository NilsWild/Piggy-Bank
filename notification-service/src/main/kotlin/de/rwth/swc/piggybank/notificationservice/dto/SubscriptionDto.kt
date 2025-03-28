package de.rwth.swc.piggybank.notificationservice.dto

import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.domain.NotificationSubscription
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object for creating a notification subscription.
 *
 * @property accountId The identifier of the account
 * @property eventType The type of event
 */
data class SubscriptionRequest(
    @field:NotBlank(message = "Account ID is required")
    val accountId: String,

    @field:NotNull(message = "Event type is required")
    val eventType: NotificationEventType
)

/**
 * Data Transfer Object for returning subscription information.
 *
 * @property id The unique identifier of the subscription
 * @property accountId The identifier of the account
 * @property eventType The type of event
 * @property active Whether the subscription is active
 * @property createdAt The timestamp when the subscription was created
 */
data class SubscriptionResponse(
    val id: UUID,
    val accountId: String,
    val eventType: NotificationEventType,
    val active: Boolean,
    val createdAt: Instant
) {
    companion object {
        /**
         * Creates a SubscriptionResponse from a domain NotificationSubscription object.
         *
         * @param subscription The domain NotificationSubscription object
         * @return The SubscriptionResponse
         */
        fun fromDomain(subscription: NotificationSubscription): SubscriptionResponse {
            return SubscriptionResponse(
                id = subscription.id,
                accountId = subscription.accountId,
                eventType = subscription.eventType,
                active = subscription.active,
                createdAt = subscription.createdAt
            )
        }
    }
}