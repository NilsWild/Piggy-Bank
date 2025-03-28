package de.rwth.swc.piggybank.accounttwinservice.repository

import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Transaction entities.
 */
@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {
    /**
     * Finds all transactions for an account.
     *
     * @param accountId The ID of the account
     * @param pageable The pagination information
     * @return A page of transactions
     */
    fun findByAccountId(accountId: String, pageable: Pageable): Page<Transaction>

    /**
     * Finds all transactions for an account.
     *
     * @param accountId The ID of the account
     * @return A list of transactions
     */
    fun findByAccountId(accountId: String): List<Transaction>

    /**
     * Finds a transaction by its transfer ID and account ID.
     *
     * @param transferId The ID of the transfer
     * @param accountId The ID of the account
     * @return The transaction, or null if not found
     */
    fun findByTransferIdAndAccountId(transferId: UUID, accountId: String): Transaction?
}