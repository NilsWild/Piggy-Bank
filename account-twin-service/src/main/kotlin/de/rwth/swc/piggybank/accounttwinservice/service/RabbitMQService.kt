package de.rwth.swc.piggybank.accounttwinservice.service

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountBalanceResponse
import de.rwth.swc.piggybank.accounttwinservice.dto.AmountDto
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

/**
 * Service for sending account-related events to RabbitMQ.
 */
@Service
class RabbitMQService(private val rabbitTemplate: RabbitTemplate) {
    private val logger = LoggerFactory.getLogger(RabbitMQService::class.java)

    companion object {
        const val EXCHANGE_NAME = "piggybank.accounts"
        const val ACCOUNT_CREATED_ROUTING_KEY = "account.created"
        const val ACCOUNT_UPDATED_ROUTING_KEY = "account.updated"
        const val ACCOUNT_DELETED_ROUTING_KEY = "account.deleted"
    }

    /**
     * Sends an account created event to RabbitMQ.
     *
     * @param account The account that was created
     */
    fun sendAccountCreatedEvent(account: Account) {
        logger.info("Sending account created event to RabbitMQ: {}", account)
        try {
            val event = mapOf(
                "eventType" to "ACCOUNT_CREATED",
                "accountId" to account.id,
                "accountType" to account.type,
                "accountIdentifier" to account.identifier,
                "balance" to AmountDto.fromDomain(account.balance)
            )
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ACCOUNT_CREATED_ROUTING_KEY, event)
            logger.info("Account created event sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send account created event to RabbitMQ", e)
            throw e
        }
    }

    /**
     * Sends an account updated event to RabbitMQ.
     *
     * @param account The account that was updated
     * @param transaction The transaction that caused the update
     */
    fun sendAccountUpdatedEvent(account: Account, transaction: Transaction) {
        logger.info("Sending account updated event to RabbitMQ: {}", account)
        try {
            val event = mapOf(
                "eventType" to "ACCOUNT_UPDATED",
                "accountId" to account.id,
                "accountType" to account.type,
                "accountIdentifier" to account.identifier,
                "balance" to AmountDto.fromDomain(account.balance),
                "transactionId" to transaction.id,
                "transactionAmount" to AmountDto.fromDomain(transaction.amount),
                "transactionType" to transaction.type.name,
                "transactionPurpose" to transaction.purpose
            )
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ACCOUNT_UPDATED_ROUTING_KEY, event)
            logger.info("Account updated event sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send account updated event to RabbitMQ", e)
            throw e
        }
    }

    /**
     * Sends an account deleted event to RabbitMQ.
     *
     * @param account The account that was deleted
     */
    fun sendAccountDeletedEvent(account: Account) {
        logger.info("Sending account deleted event to RabbitMQ: {}", account)
        try {
            val event = mapOf(
                "eventType" to "ACCOUNT_DELETED",
                "accountId" to account.id,
                "accountType" to account.type,
                "accountIdentifier" to account.identifier
            )
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ACCOUNT_DELETED_ROUTING_KEY, event)
            logger.info("Account deleted event sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send account deleted event to RabbitMQ", e)
            throw e
        }
    }
}