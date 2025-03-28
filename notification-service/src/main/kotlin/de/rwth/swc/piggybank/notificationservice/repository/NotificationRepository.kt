package de.rwth.swc.piggybank.notificationservice.repository

import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for managing notifications.
 */
@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    /**
     * Finds all notifications for a specific account.
     *
     * @param accountId The identifier of the account
     * @param pageable The pagination information
     * @return A page of notifications
     */
    fun findByAccountId(accountId: String, pageable: Pageable): Page<Notification>

    /**
     * Finds all unread notifications.
     *
     * @param pageable The pagination information
     * @return A page of unread notifications
     */
    fun findByReadFalse(pageable: Pageable): Page<Notification>

    /**
     * Finds all notifications for a specific event type.
     *
     * @param eventType The type of event
     * @param pageable The pagination information
     * @return A page of notifications
     */
    fun findByEventType(eventType: NotificationEventType, pageable: Pageable): Page<Notification>

    /**
     * Finds all unread notifications for a specific account.
     *
     * @param accountId The identifier of the account
     * @param pageable The pagination information
     * @return A page of unread notifications
     */
    fun findByAccountIdAndReadFalse(accountId: String, pageable: Pageable): Page<Notification>

    /**
     * Counts the number of unread notifications.
     *
     * @return The number of unread notifications
     */
    fun countByReadFalse(): Long
}
