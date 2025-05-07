package de.rwth.swc.piggybank.transfergateway.dto

import de.rwth.swc.piggybank.transfergateway.domain.Amount
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object for returning transaction information.
 *
 * @property id The unique identifier of the transaction
 * @property transferId The identifier of the transfer that generated this transaction
 * @property affectedAccountId The identifier of the account affected by the transaction
 * @property amount The amount of the transaction
 * @property valuationTimestamp The timestamp when the transaction was valued
 * @property purpose The purpose or description of the transaction
 * @property type The type of the transaction (CREDIT or DEBIT)
 * @property sourceAccount The account from which the money was received (for CREDIT transactions)
 * @property destinationAccount The account to which the money was sent (for DEBIT transactions)
 * @property createdAt The timestamp when the transaction was created
 */
data class TransactionResponse(
    val id: UUID,
    val transferId: UUID,
    val affectedAccountId: String,
    val amount: Amount,
    val valuationTimestamp: Instant,
    val purpose: String,
    val type: String,
    val sourceAccount: String?,
    val destinationAccount: String?,
    val createdAt: Instant
)