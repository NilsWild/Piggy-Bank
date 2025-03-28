package de.rwth.swc.piggybank.accounttwinservice.service

import de.rwth.swc.piggybank.accounttwinservice.client.TransferGatewayClient
import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.domain.TransactionType
import de.rwth.swc.piggybank.accounttwinservice.repository.AccountRepository
import de.rwth.swc.piggybank.accounttwinservice.repository.TransactionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Service for managing accounts.
 */
@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val rabbitMQService: RabbitMQService,
    private val transferGatewayClient: TransferGatewayClient
) {
    private val logger = LoggerFactory.getLogger(AccountService::class.java)

    /**
     * Creates a new monitored account.
     *
     * @param account The account to create
     * @return The created account
     * @throws IllegalStateException If an account with the same type and identifier already exists
     */
    @Transactional
    fun createAccount(account: Account): Account {
        logger.info("Creating account: {}", account)

        if (accountRepository.existsByTypeAndIdentifier(account.type, account.identifier)) {
            throw IllegalStateException("Account with type ${account.type} and identifier ${account.identifier} already exists")
        }

        val savedAccount = accountRepository.save(account)

        // Create an initial transaction for the account
        val initialTransaction = Transaction(
            id = UUID.randomUUID(),
            transferId = UUID.randomUUID(),
            accountId = savedAccount.id,
            account = savedAccount,
            amount = savedAccount.balance,
            valuationTimestamp = Instant.now(),
            purpose = "Initial balance",
            type = TransactionType.DUMMY
        )

        transactionRepository.save(initialTransaction)

        // Emit account creation event
        rabbitMQService.sendAccountCreatedEvent(savedAccount)

        // Add the account to the monitored accounts in the TransferGateway
        try {
            val added = transferGatewayClient.addMonitoredAccount(savedAccount)
            logger.info("Account {} added to monitored accounts in TransferGateway", if (added) "successfully" else "already exists")
        } catch (e: Exception) {
            logger.error("Failed to add account to monitored accounts in TransferGateway", e)
            // We don't want to fail the account creation if the TransferGateway is unavailable,
            // so we just log the error and continue
        }

        return savedAccount
    }

    /**
     * Gets an account by its ID.
     *
     * @param accountId The ID of the account
     * @return The account, or null if not found
     */
    fun getAccount(accountId: String): Account? {
        logger.info("Getting account with ID: {}", accountId)
        return accountRepository.findById(accountId).orElse(null)
    }

    /**
     * Gets an account by its type and identifier.
     *
     * @param type The type of the account
     * @param identifier The identifier of the account
     * @return The account, or null if not found
     */
    fun getAccountByTypeAndIdentifier(type: String, identifier: String): Account? {
        logger.info("Getting account with type: {} and identifier: {}", type, identifier)
        return accountRepository.findByTypeAndIdentifier(type, identifier)
    }

    /**
     * Gets all accounts.
     *
     * @return A list of all accounts
     */
    fun getAllAccounts(): List<Account> {
        logger.info("Getting all accounts")
        return accountRepository.findAll()
    }

    /**
     * Gets all transactions for an account.
     *
     * @param accountId The ID of the account
     * @param pageable The pagination information
     * @return A page of transactions
     */
    fun getAccountTransactions(accountId: String, pageable: Pageable): Page<Transaction> {
        logger.info("Getting transactions for account with ID: {}", accountId)
        return transactionRepository.findByAccountId(accountId, pageable)
    }

    /**
     * Deletes an account by its ID.
     *
     * @param accountId The ID of the account
     * @return true if the account was deleted, false if it was not found
     */
    @Transactional
    fun deleteAccount(accountId: String): Boolean {
        logger.info("Deleting account with ID: {}", accountId)

        val account = accountRepository.findById(accountId).orElse(null) ?: return false

        // Delete all transactions for the account
        val transactions = transactionRepository.findByAccountId(accountId)
        transactionRepository.deleteAll(transactions)

        // Delete the account
        accountRepository.delete(account)

        // Emit account deletion event
        rabbitMQService.sendAccountDeletedEvent(account)

        return true
    }

    /**
     * Updates an account's balance with a transaction.
     *
     * @param transaction The transaction to apply
     * @return The updated account
     */
    @Transactional
    fun updateAccountBalance(transaction: Transaction): Account {
        logger.info("Updating account balance with transaction: {}", transaction)

        val account = transaction.account
        val updatedAccount = transaction.updateAccountBalance()

        // Save the transaction
        val savedTransaction = transactionRepository.save(transaction)

        // Update the account
        val savedAccount = accountRepository.save(updatedAccount)

        // Emit account update event
        rabbitMQService.sendAccountUpdatedEvent(savedAccount, savedTransaction)

        return savedAccount
    }
}
