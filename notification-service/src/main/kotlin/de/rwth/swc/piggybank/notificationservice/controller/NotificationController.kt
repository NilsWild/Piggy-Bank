package de.rwth.swc.piggybank.notificationservice.controller

import de.rwth.swc.piggybank.notificationservice.dto.NotificationResponse
import de.rwth.swc.piggybank.notificationservice.dto.PageResponse
import de.rwth.swc.piggybank.notificationservice.dto.UnreadNotificationCount
import de.rwth.swc.piggybank.notificationservice.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Controller for managing notifications.
 */
@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val notificationService: NotificationService) {
    private val logger = LoggerFactory.getLogger(NotificationController::class.java)

    /**
     * Gets all notifications.
     *
     * @param page The page number (0-based)
     * @param size The page size
     * @return A page of notifications
     */
    @GetMapping
    fun getAllNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PageResponse<NotificationResponse>> {
        logger.info("Getting all notifications, page: {}, size: {}", page, size)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val notifications = notificationService.getAllNotifications(pageable)
        return ResponseEntity.ok(PageResponse.fromPage(notifications, NotificationResponse::fromDomain))
    }

    /**
     * Gets all notifications for a specific account.
     *
     * @param accountId The identifier of the account
     * @param page The page number (0-based)
     * @param size The page size
     * @return A page of notifications
     */
    @GetMapping("/account/{accountId}")
    fun getAccountNotifications(
        @PathVariable accountId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PageResponse<NotificationResponse>> {
        logger.info("Getting notifications for account: {}, page: {}, size: {}", accountId, page, size)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val notifications = notificationService.getAccountNotifications(accountId, pageable)
        return ResponseEntity.ok(PageResponse.fromPage(notifications, NotificationResponse::fromDomain))
    }

    /**
     * Gets all unread notifications.
     *
     * @param page The page number (0-based)
     * @param size The page size
     * @return A page of unread notifications
     */
    @GetMapping("/unread")
    fun getUnreadNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PageResponse<NotificationResponse>> {
        logger.info("Getting unread notifications, page: {}, size: {}", page, size)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val notifications = notificationService.getUnreadNotifications(pageable)
        return ResponseEntity.ok(PageResponse.fromPage(notifications, NotificationResponse::fromDomain))
    }

    /**
     * Gets all unread notifications for a specific account.
     *
     * @param accountId The identifier of the account
     * @param page The page number (0-based)
     * @param size The page size
     * @return A page of unread notifications
     */
    @GetMapping("/account/{accountId}/unread")
    fun getUnreadNotificationsForAccount(
        @PathVariable accountId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PageResponse<NotificationResponse>> {
        logger.info("Getting unread notifications for account: {}, page: {}, size: {}", accountId, page, size)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val notifications = notificationService.getUnreadNotificationsForAccount(accountId, pageable)
        return ResponseEntity.ok(PageResponse.fromPage(notifications, NotificationResponse::fromDomain))
    }

    /**
     * Counts the number of unread notifications.
     *
     * @return The number of unread notifications
     */
    @GetMapping("/count")
    fun countUnreadNotifications(): ResponseEntity<UnreadNotificationCount> {
        logger.info("Counting unread notifications")
        val count = notificationService.countUnreadNotifications()
        return ResponseEntity.ok(UnreadNotificationCount(count))
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId The identifier of the notification
     * @return No content if successful, not found if the notification was not found
     */
    @PostMapping("/{notificationId}/read")
    fun markNotificationAsRead(@PathVariable notificationId: UUID): ResponseEntity<Void> {
        logger.info("Marking notification as read: {}", notificationId)
        val marked = notificationService.markNotificationAsRead(notificationId)
        return if (marked) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}