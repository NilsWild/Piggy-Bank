package de.rwth.swc.piggybank.accounttwinservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * Represents a transaction in the system.
 * A transaction is a credit or debit operation on a single account.
 *
 * @property id The unique identifier of the transaction
 * @property transferId The identifier of the transfer that generated this transaction
 * @property accountId The identifier of the account involved in the transaction
 * @property account The account involved in the transaction
 * @property amount The amount of the transaction
 * @property valuationTimestamp The timestamp when the transaction was valued
 * @property purpose The purpose or description of the transaction
 * @property type The type of the transaction (CREDIT, DEBIT, or DUMMY)
 * @property sourceAccount The account from which the money was received (for CREDIT transactions)
 * @property destinationAccount The account to which the money was sent (for DEBIT transactions)
 * @property createdAt The timestamp when the transaction was created
 */
@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "transfer_id", nullable = false)
    val transferId: UUID,

    @Column(name = "account_id", nullable = false, insertable = false, updatable = false)
    val accountId: String,

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Embedded
    val amount: Amount,

    @Column(name = "valuation_timestamp", nullable = false)
    val valuationTimestamp: Instant,

    @Column(name = "purpose", nullable = false)
    val purpose: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: TransactionType,

    @Column(name = "source_account", nullable = true)
    val sourceAccount: String? = null,

    @Column(name = "destination_account", nullable = true)
    val destinationAccount: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
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

    /**
     * Updates the account balance with this transaction.
     *
     * @return The updated account
     */
    fun updateAccountBalance(): Account {
        return account.updateBalance(amount, type)
    }
}
