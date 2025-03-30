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
     * @param affectedAccountId The ID of the affected account
     * @param pageable The pagination information
     * @return A page of transactions
     */
    fun findByAffectedAccountId(affectedAccountId: String, pageable: Pageable): Page<Transaction>

    /**
     * Finds all transactions for an account.
     *
     * @param affectedAccountId The ID of the affected account
     * @return A list of transactions
     */
    fun findByAffectedAccountId(affectedAccountId: String): List<Transaction>

    /**
     * Finds a transaction by its transfer ID and affected account ID.
     *
     * @param transferId The ID of the transfer
     * @param affectedAccountId The ID of the affected account
     * @return The transaction, or null if not found
     */
    fun findByTransferIdAndAffectedAccountId(transferId: UUID, affectedAccountId: String): Transaction?
}
