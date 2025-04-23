package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.config.RabbitMQConfig
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.ClassificationEvent
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Service for listening to classification events from RabbitMQ.
 * Processes the events and updates goals accordingly.
 */
@Service
class ClassificationListener(
    private val goalRepository: GoalRepository,
    private val rabbitMQService: RabbitMQService,
    private val transferClassificationCache: TransferClassificationCache
) {
    private val logger = LoggerFactory.getLogger(ClassificationListener::class.java)

    /**
     * Listens for classification events from RabbitMQ.
     * Updates goals based on the classification information in the event.
     *
     * @param event The classification event
     */
    @RabbitListener(queues = [RabbitMQConfig.CLASSIFICATION_QUEUE_NAME])
    @Transactional
    fun handleClassificationEvent(event: ClassificationEvent) {
        logger.info("Received classification event: {}", event)

        try {
            val transferId = event.transferId
            val classifications = event.classifications

            // Store the classifications in the cache
            transferClassificationCache.storeClassifications(transferId, classifications)

            // Check if transfer information is available
            if (!transferClassificationCache.hasTransferInfo(transferId)) {
                logger.info("Transfer information not found in cache for transfer ID: {}. Storing classifications for later processing.", transferId)
                return
            }

            // Retrieve transfer information from the cache
            val accountId = transferClassificationCache.getAccountId(transferId)
            val amount = transferClassificationCache.getAmount(transferId)
            val type = transferClassificationCache.getType(transferId)
            val purpose = transferClassificationCache.getPurpose(transferId)

            if (accountId == null || amount == null || type == null || purpose == null) {
                logger.warn("Transfer information incomplete in cache for transfer ID: {}", transferId)
                return
            }

            // Find all active goals for the account
            val activeGoals = goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE)
            logger.info("Found {} active goals for account {}", activeGoals.size, accountId)

            // Process the classification for each goal
            // Only process SpendingLimitGoal goals, as other goals have already been processed by the AccountUpdateListener
            val updatedGoals = activeGoals.filter { goal ->
                if (goal is SpendingLimitGoal) {
                    // Process the account update with classifications and check if the goal's status changed
                    goal.processAccountUpdate(
                        accountId = accountId,
                        transactionAmount = BigDecimal(amount),
                        transactionType = type,
                        transactionPurpose = purpose,
                        classifications = classifications
                    )
                } else {
                    // Skip non-SpendingLimitGoal goals as they have already been processed by the AccountUpdateListener
                    false
                }
            }

            // Save the updated goals
            if (updatedGoals.isNotEmpty()) {
                goalRepository.saveAll(updatedGoals)
                logger.info("Updated {} goals", updatedGoals.size)

                // Send goal status events for goals that have changed status
                updatedGoals.forEach { goal ->
                    rabbitMQService.sendGoalStatusEvent(goal)
                }
            }

            // Remove transfer information from the cache
            transferClassificationCache.removeTransferInfo(transferId)
        } catch (e: Exception) {
            logger.error("Error processing classification event", e)
            throw e
        }
    }
}
