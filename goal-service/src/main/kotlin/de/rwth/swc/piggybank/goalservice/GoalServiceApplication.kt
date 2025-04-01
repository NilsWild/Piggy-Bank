package de.rwth.swc.piggybank.goalservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main application class for the Goal Service.
 * This service tracks financial goals based on account updates and transfer classifications.
 */
@SpringBootApplication
class GoalServiceApplication

/**
 * Main function that starts the Goal Service application.
 *
 * @param args Command line arguments
 */
fun main(args: Array<String>) {
    runApplication<GoalServiceApplication>(*args)
}