package de.rwth.swc.piggybank.transfergateway.domain

import java.time.Instant
import java.util.UUID

/**
 * Represents a transaction in the system.
 * A transaction is a credit or debit operation on a single account, derived from a Transfer.
 *
 * @property id The unique identifier of the transaction
 * @property transferId The identifier of the transfer that generated this transaction
 * @property account The account involved in the transaction
 * @property amount The amount of the transaction
 * @property valuationTimestamp The timestamp when the transaction was valued
 * @property purpose The purpose or description of the transaction
 * @property type The type of the transaction (CREDIT, DEBIT, or DUMMY)
 * @property sourceAccount The account from which the money was received (for CREDIT transactions)
 * @property destinationAccount The account to which the money was sent (for DEBIT transactions)
 */
data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val transferId: UUID,
    val account: Account,
    val amount: Amount,
    val valuationTimestamp: Instant,
    val purpose: String,
    val type: TransactionType,
    val sourceAccount: String? = null,
    val destinationAccount: String? = null
) {
    /**
     * Checks if this transaction is a credit transaction.
     *
     * @return true if the transaction is a credit, false otherwise
     */
    fun isCredit(): Boolean = type == TransactionType.CREDIT

    /**
     * Checks if this transaction is a debit transaction.
     *
     * @return true if the transaction is a debit, false otherwise
     */
    fun isDebit(): Boolean = type == TransactionType.DEBIT
}
