package de.rwth.swc.piggybank.notificationservice.controller

import de.rwth.swc.piggybank.notificationservice.dto.SubscriptionRequest
import de.rwth.swc.piggybank.notificationservice.dto.SubscriptionResponse
import de.rwth.swc.piggybank.notificationservice.service.NotificationService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Controller for managing notification subscriptions.
 */
@RestController
@RequestMapping("/api/subscriptions")
class NotificationSubscriptionController(private val notificationService: NotificationService) {
    private val logger = LoggerFactory.getLogger(NotificationSubscriptionController::class.java)

    /**
     * Creates a new notification subscription.
     *
     * @param request The subscription request
     * @return The created subscription
     */
    @PostMapping
    fun createSubscription(@Valid @RequestBody request: SubscriptionRequest): ResponseEntity<SubscriptionResponse> {
        logger.info("Creating subscription: {}", request)
        val subscription = notificationService.createSubscription(
            accountId = request.accountId,
            eventType = request.eventType
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriptionResponse.fromDomain(subscription))
    }

    /**
     * Gets all active subscriptions.
     *
     * @return A list of active subscriptions
     */
    @GetMapping
    fun getAllSubscriptions(): ResponseEntity<List<SubscriptionResponse>> {
        logger.info("Getting all active subscriptions")
        val subscriptions = notificationService.getAllSubscriptions()
        return ResponseEntity.ok(subscriptions.map { SubscriptionResponse.fromDomain(it) })
    }

    /**
     * Gets all active subscriptions for an account.
     *
     * @param accountId The identifier of the account
     * @return A list of active subscriptions
     */
    @GetMapping("/account/{accountId}")
    fun getAccountSubscriptions(@PathVariable accountId: String): ResponseEntity<List<SubscriptionResponse>> {
        logger.info("Getting subscriptions for account: {}", accountId)
        val subscriptions = notificationService.getAccountSubscriptions(accountId)
        return ResponseEntity.ok(subscriptions.map { SubscriptionResponse.fromDomain(it) })
    }

    /**
     * Deactivates a notification subscription.
     *
     * @param subscriptionId The identifier of the subscription
     * @return No content if successful, not found if the subscription was not found
     */
    @DeleteMapping("/{subscriptionId}")
    fun deactivateSubscription(@PathVariable subscriptionId: UUID): ResponseEntity<Void> {
        logger.info("Deactivating subscription: {}", subscriptionId)
        val deactivated = notificationService.deactivateSubscription(subscriptionId)
        return if (deactivated) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}