package de.rwth.swc.piggybank.transfergateway.dto

import de.rwth.swc.piggybank.transfergateway.domain.Amount
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object for sending transactions to the AccountTwinService.
 * This class matches the expected format of the AccountTwinService's TransactionRequest.
 *
 * @property id The unique identifier of the transaction
 * @property transferId The identifier of the transfer that generated this transaction
 * @property accountId The identifier of the account involved in the transaction
 * @property amount The amount of the transaction
 * @property valuationTimestamp The timestamp when the transaction was valued
 * @property purpose The purpose or description of the transaction
 * @property type The type of the transaction (CREDIT or DEBIT)
 */
data class AccountTwinServiceTransactionRequest(
    val id: UUID,
    val transferId: UUID,
    val accountId: String,
    val amount: Amount,
    val valuationTimestamp: Instant,
    val purpose: String,
    val type: String,
    val sourceAccount: String? = null,
    val destinationAccount: String? = null
) {
    companion object {
        /**
         * Creates an AccountTwinServiceTransactionRequest from a TransactionRequest.
         *
         * @param transactionRequest The TransactionRequest to convert
         * @return The AccountTwinServiceTransactionRequest
         */
        fun fromTransactionRequest(transactionRequest: TransactionRequest): AccountTwinServiceTransactionRequest {
            return AccountTwinServiceTransactionRequest(
                id = transactionRequest.id,
                transferId = transactionRequest.transferId,
                accountId = transactionRequest.account.accountId ?: transactionRequest.account.toString(),
                amount = transactionRequest.amount,
                valuationTimestamp = transactionRequest.valuationTimestamp,
                purpose = transactionRequest.purpose,
                type = transactionRequest.type,
                sourceAccount = transactionRequest.sourceAccount,
                destinationAccount = transactionRequest.destinationAccount
            )
        }
    }
}
