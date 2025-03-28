package de.rwth.swc.piggybank.transfergateway.dto

import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.domain.Transaction
import de.rwth.swc.piggybank.transfergateway.domain.TransactionType
import java.time.Instant
import java.util.UUID
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

/**
 * Data Transfer Object for sending transactions to the AccountTwinService.
 *
 * @property id The unique identifier of the transaction
 * @property transferId The identifier of the transfer that generated this transaction
 * @property account The account involved in the transaction
 * @property amount The amount of the transaction
 * @property valuationTimestamp The timestamp when the transaction was valued
 * @property purpose The purpose or description of the transaction
 * @property type The type of the transaction (CREDIT or DEBIT)
 */
data class TransactionRequest(
    val id: UUID,
    val transferId: UUID,
    @field:Valid
    @field:NotNull(message = "Account is required")
    val account: Account,
    @field:Valid
    @field:NotNull(message = "Amount is required")
    val amount: Amount,
    val valuationTimestamp: Instant,
    val purpose: String,
    val type: String,
    val sourceAccount: String? = null,
    val destinationAccount: String? = null
) {
    companion object {
        /**
         * Creates a TransactionRequest from a domain Transaction object.
         *
         * @param transaction The domain Transaction object
         * @return The TransactionRequest DTO
         */
        fun fromDomain(transaction: Transaction): TransactionRequest {
            return TransactionRequest(
                id = transaction.id,
                transferId = transaction.transferId,
                account = transaction.account,
                amount = transaction.amount,
                valuationTimestamp = transaction.valuationTimestamp,
                purpose = transaction.purpose,
                type = transaction.type.name,
                sourceAccount = transaction.sourceAccount,
                destinationAccount = transaction.destinationAccount
            )
        }
    }
}
