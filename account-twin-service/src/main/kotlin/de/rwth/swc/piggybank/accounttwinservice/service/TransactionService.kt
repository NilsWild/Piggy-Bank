package de.rwth.swc.piggybank.accounttwinservice.service

import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionRequest
import de.rwth.swc.piggybank.accounttwinservice.repository.AccountRepository
import de.rwth.swc.piggybank.accounttwinservice.repository.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service for processing transactions.
 */
@Service
class TransactionService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val accountService: AccountService
) {
    private val logger = LoggerFactory.getLogger(TransactionService::class.java)

    /**
     * Processes a transaction request.
     *
     * @param transactionRequest The transaction request to process
     * @return The processed transaction
     * @throws IllegalStateException If the account does not exist
     */
    @Transactional
    fun processTransaction(transactionRequest: TransactionRequest): Transaction {
        logger.info("Processing transaction request: {}", transactionRequest)

        // Validate that the affectedAccountId matches either sourceAccount or destinationAccount
        when (transactionRequest.type) {
            "CREDIT" -> {
                if (transactionRequest.destinationAccount != transactionRequest.affectedAccountId) {
                    throw IllegalStateException("For CREDIT transactions, the affected account ID must match the destination account")
                }
            }
            "DEBIT" -> {
                if (transactionRequest.sourceAccount != transactionRequest.affectedAccountId) {
                    throw IllegalStateException("For DEBIT transactions, the affected account ID must match the source account")
                }
            }
            else -> {
                // For DUMMY transactions or other types, no validation is needed
            }
        }

        val account = accountRepository.findById(transactionRequest.affectedAccountId)
            .orElseThrow { IllegalStateException("Account with ID ${transactionRequest.affectedAccountId} not found") }

        val transaction = transactionRequest.toDomain(account)

        // Process the transaction and update the account balance
        accountService.updateAccountBalance(transaction)

        return transaction
    }

    /**
     * Gets a transaction by its ID.
     *
     * @param transactionId The ID of the transaction
     * @return The transaction, or null if not found
     */
    fun getTransaction(transactionId: UUID): Transaction? {
        logger.info("Getting transaction with ID: {}", transactionId)
        return transactionRepository.findById(transactionId).orElse(null)
    }

    /**
     * Gets all transactions for an account.
     *
     * @param affectedAccountId The ID of the affected account
     * @param pageable The pagination information
     * @return A page of transactions
     */
    fun getTransactionsByAccount(affectedAccountId: String, pageable: Pageable): Page<Transaction> {
        logger.info("Getting transactions for account with ID: {}", affectedAccountId)
        return transactionRepository.findByAffectedAccountId(affectedAccountId, pageable)
    }

    /**
     * Gets a transaction by its transfer ID and affected account ID.
     *
     * @param transferId The ID of the transfer
     * @param affectedAccountId The ID of the affected account
     * @return The transaction, or null if not found
     */
    fun getTransactionByTransferIdAndAccountId(transferId: Long, affectedAccountId: String): Transaction? {
        logger.info("Getting transaction with transfer ID: {} and affected account ID: {}", transferId, affectedAccountId)
        return transactionRepository.findByTransferIdAndAffectedAccountId(transferId, affectedAccountId)
    }

    /**
     * Checks if a transaction with the given transfer ID and affected account ID already exists.
     *
     * @param transferId The ID of the transfer
     * @param affectedAccountId The ID of the affected account
     * @return true if the transaction exists, false otherwise
     */
    fun transactionExists(transferId: Long, affectedAccountId: String): Boolean {
        logger.info("Checking if transaction with transfer ID: {} and affected account ID: {} exists", transferId, affectedAccountId)
        return transactionRepository.findByTransferIdAndAffectedAccountId(transferId, affectedAccountId) != null
    }
}
