package de.rwth.swc.piggybank.transfergateway.service

import de.rwth.swc.piggybank.transfergateway.client.AccountTwinServiceClient
import de.rwth.swc.piggybank.transfergateway.domain.Transfer
import de.rwth.swc.piggybank.transfergateway.dto.TransactionRequest
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

/**
 * Service for processing transfers and sending transactions to the AccountTwinService.
 */
@Service
class TransferService(
    private val accountService: AccountService,
    private val rabbitMQService: RabbitMQService,
    private val accountTwinServiceClient: AccountTwinServiceClient
) {
    private val logger = LoggerFactory.getLogger(TransferService::class.java)

    /**
     * Processes a transfer.
     * If the source or target account is monitored, sends a transfer event to RabbitMQ
     * and sends the corresponding transactions to the AccountTwinService.
     *
     * @param transfer The transfer to process
     * @return true if the transfer was processed successfully, false otherwise
     */
    fun processTransfer(transfer: Transfer): Boolean {
        val sourceAccountMonitored = accountService.isMonitored(transfer.sourceAccount)
        val targetAccountMonitored = accountService.isMonitored(transfer.targetAccount)

        if (!sourceAccountMonitored && !targetAccountMonitored) {
            logger.info("Neither source nor target account is monitored, skipping transfer processing")
            return true
        }

        try {
            // Send transfer event to RabbitMQ
            rabbitMQService.sendTransferEvent(transfer)

            // Send transactions to AccountTwinService
            if (sourceAccountMonitored) {
                val debitTransaction = transfer.toDebitTransaction()
                sendTransactionToAccountTwinService(TransactionRequest.fromDomain(debitTransaction))
            }

            if (targetAccountMonitored) {
                val creditTransaction = transfer.toCreditTransaction()
                sendTransactionToAccountTwinService(TransactionRequest.fromDomain(creditTransaction))
            }

            return true
        } catch (e: Exception) {
            logger.error("Failed to process transfer", e)
            return false
        }
    }

    /**
     * Sends a transaction to the AccountTwinService.
     *
     * @param transactionRequest The transaction request to send
     */
    private fun sendTransactionToAccountTwinService(transactionRequest: TransactionRequest) {
        logger.info("Sending transaction to AccountTwinService: {}", transactionRequest)
        try {
            accountTwinServiceClient.sendTransaction(transactionRequest)
            logger.info("Transaction sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send transaction to AccountTwinService", e)
            throw e
        }
    }
}
