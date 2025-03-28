package de.rwth.swc.piggybank.notificationservice.domain

/**
 * Represents the types of events for which users can subscribe to notifications.
 */
enum class NotificationEventType {
    /**
     * Represents a balance update event (money received or sent).
     */
    BALANCE_UPDATE,

    /**
     * Represents an account created event.
     */
    ACCOUNT_CREATED,

    /**
     * Represents an account deleted event.
     */
    ACCOUNT_DELETED
}