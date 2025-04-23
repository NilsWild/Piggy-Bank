package de.rwth.swc.piggybank.goalservice.service

import de.rwth.swc.piggybank.goalservice.config.RabbitMQConfig
import de.rwth.swc.piggybank.goalservice.domain.GoalStatus
import de.rwth.swc.piggybank.goalservice.domain.SpendingLimitGoal
import de.rwth.swc.piggybank.goalservice.dto.AccountUpdatedEvent
import de.rwth.swc.piggybank.goalservice.repository.GoalRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

/**
 * Service for listening to account update events from RabbitMQ.
 * Processes the events and updates goals accordingly.
 */
@Service
class AccountUpdateListener(
    private val goalRepository: GoalRepository,
    private val rabbitMQService: RabbitMQService,
    private val transferClassificationCache: TransferClassificationCache
) {
    private val logger = LoggerFactory.getLogger(AccountUpdateListener::class.java)

    /**
     * Listens for account updated events from RabbitMQ.
     * Updates goals based on the transaction information in the event.
     *
     * @param event The account updated event
     */
    @RabbitListener(queues = [RabbitMQConfig.ACCOUNT_UPDATED_QUEUE_NAME])
    @Transactional
    fun handleAccountUpdatedEvent(event: AccountUpdatedEvent) {
        logger.info("Received account updated event: {}", event)

        try {
            // Extract transaction information from the event
            val accountId = event.accountId
            val transactionAmount = BigDecimal(event.transactionAmount.value)
            val transactionType = event.transactionType
            val transactionPurpose = event.transactionPurpose
            val transactionId = UUID.fromString(event.transactionId)
            val transferId = UUID.fromString(event.transferId)

            // Store transfer information in the cache for later use by the ClassificationListener
            transferClassificationCache.storeTransferInfo(
                transferId = transferId,
                accountId = accountId,
                amount = event.transactionAmount.value,
                type = transactionType,
                purpose = transactionPurpose
            )

            // Map the transaction ID to the transfer ID for correlation
            transferClassificationCache.mapTransactionToTransfer(transactionId, transferId)

            // Find all active goals for the account
            val activeGoals = goalRepository.findByAccountIdAndStatus(accountId, GoalStatus.ACTIVE)
            logger.info("Found {} active goals for account {}", activeGoals.size, accountId)

            // Check if classifications are available for this transfer
            val classifications = transferClassificationCache.getClassifications(transferId) ?: emptyList()
            val hasClassifications = classifications.isNotEmpty()

            // Process the transaction for each goal
            val updatedGoals = activeGoals.filter { goal ->
                // Skip SpendingLimitGoal if classifications are not available
                if (goal is SpendingLimitGoal && !hasClassifications) {
                    logger.info("Skipping SpendingLimitGoal {} as classifications are not available yet", goal.id)
                    false
                } else {
                    // Process the account update and check if the goal's status changed
                    goal.processAccountUpdate(
                        accountId = accountId,
                        transactionAmount = transactionAmount,
                        transactionType = transactionType,
                        transactionPurpose = transactionPurpose,
                        classifications = classifications
                    )
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

            // If we processed all goals (including SpendingLimitGoal), we can remove the transfer information from the cache
            if (hasClassifications || activeGoals.none { it is SpendingLimitGoal }) {
                transferClassificationCache.removeTransferInfo(transferId)
                logger.info("Removed transfer information from cache for transfer ID: {}", transferId)
            }
        } catch (e: Exception) {
            logger.error("Error processing account updated event", e)
            throw e
        }
    }
}
