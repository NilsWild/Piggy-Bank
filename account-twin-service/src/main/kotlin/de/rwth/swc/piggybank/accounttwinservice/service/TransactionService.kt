package de.rwth.swc.piggybank.accounttwinservice.service

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
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

        val account = accountRepository.findById(transactionRequest.accountId)
            .orElseThrow { IllegalStateException("Account with ID ${transactionRequest.accountId} not found") }

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
     * @param accountId The ID of the account
     * @param pageable The pagination information
     * @return A page of transactions
     */
    fun getTransactionsByAccount(accountId: String, pageable: Pageable): Page<Transaction> {
        logger.info("Getting transactions for account with ID: {}", accountId)
        return transactionRepository.findByAccountId(accountId, pageable)
    }

    /**
     * Gets a transaction by its transfer ID and account ID.
     *
     * @param transferId The ID of the transfer
     * @param accountId The ID of the account
     * @return The transaction, or null if not found
     */
    fun getTransactionByTransferIdAndAccountId(transferId: UUID, accountId: String): Transaction? {
        logger.info("Getting transaction with transfer ID: {} and account ID: {}", transferId, accountId)
        return transactionRepository.findByTransferIdAndAccountId(transferId, accountId)
    }

    /**
     * Checks if a transaction with the given transfer ID and account ID already exists.
     *
     * @param transferId The ID of the transfer
     * @param accountId The ID of the account
     * @return true if the transaction exists, false otherwise
     */
    fun transactionExists(transferId: UUID, accountId: String): Boolean {
        logger.info("Checking if transaction with transfer ID: {} and account ID: {} exists", transferId, accountId)
        return transactionRepository.findByTransferIdAndAccountId(transferId, accountId) != null
    }
}
