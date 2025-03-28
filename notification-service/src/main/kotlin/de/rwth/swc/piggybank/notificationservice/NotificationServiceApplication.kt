package de.rwth.swc.piggybank.notificationservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main application class for the NotificationService.
 * This service listens for account update events, checks user notification preferences,
 * generates notifications, and sends them to the UI via AMQP.
 */
@SpringBootApplication
class NotificationServiceApplication

/**
 * Main function to start the NotificationService application.
 */
fun main(args: Array<String>) {
    runApplication<NotificationServiceApplication>(*args)
}