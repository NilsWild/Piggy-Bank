package de.rwth.swc.piggybank.accounttwinservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main application class for the AccountTwinService.
 * This service keeps copies of monitored accounts, processes transactions,
 * and emits account updates via AMQP.
 */
@SpringBootApplication
class AccountTwinServiceApplication

/**
 * Main function to start the AccountTwinService application.
 */
fun main(args: Array<String>) {
    runApplication<AccountTwinServiceApplication>(*args)
}