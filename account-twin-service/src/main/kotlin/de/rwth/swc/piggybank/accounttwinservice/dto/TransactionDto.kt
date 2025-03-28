package de.rwth.swc.piggybank.accounttwinservice.dto

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.domain.TransactionType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object for creating a transaction.
 *
 * @property id The unique identifier of the transaction
 * @property transferId The identifier of the transfer that generated this transaction
 * @property accountId The identifier of the account involved in the transaction
 * @property amount The amount of the transaction
 * @property valuationTimestamp The timestamp when the transaction was valued
 * @property purpose The purpose or description of the transaction
 * @property type The type of the transaction (CREDIT or DEBIT)
 */
data class TransactionRequest(
    val id: UUID = UUID.randomUUID(),
    val transferId: UUID,
    @field:NotBlank(message = "Account ID is required")
    val accountId: String,
    @field:Valid
    @field:NotNull(message = "Amount is required")
    val amount: AmountDto,
    val valuationTimestamp: Instant,
    val purpose: String,
    @field:NotBlank(message = "Transaction type is required")
    val type: String,
    val sourceAccount: String? = null,
    val destinationAccount: String? = null
) {
    /**
     * Converts this DTO to a domain Transaction object.
     *
     * @param account The domain Account object
     * @return The domain Transaction object
     */
    fun toDomain(account: Account): Transaction {
        return Transaction(
            id = id,
            transferId = transferId,
            accountId = accountId,
            account = account,
            amount = amount.toDomain(),
            valuationTimestamp = valuationTimestamp,
            purpose = purpose,
            type = TransactionType.valueOf(type),
            sourceAccount = sourceAccount,
            destinationAccount = destinationAccount
        )
    }
}

/**
 * Data Transfer Object for returning transaction information.
 *
 * @property id The unique identifier of the transaction
 * @property transferId The identifier of the transfer that generated this transaction
 * @property accountId The identifier of the account involved in the transaction
 * @property amount The amount of the transaction
 * @property valuationTimestamp The timestamp when the transaction was valued
 * @property purpose The purpose or description of the transaction
 * @property type The type of the transaction (CREDIT or DEBIT)
 * @property createdAt The timestamp when the transaction was created
 */
data class TransactionResponse(
    val id: UUID,
    val transferId: UUID,
    val accountId: String,
    val amount: AmountDto,
    val valuationTimestamp: Instant,
    val purpose: String,
    val type: String,
    val sourceAccount: String?,
    val destinationAccount: String?,
    val createdAt: Instant
) {
    companion object {
        /**
         * Creates a TransactionResponse from a domain Transaction object.
         *
         * @param transaction The domain Transaction object
         * @return The TransactionResponse
         */
        fun fromDomain(transaction: Transaction): TransactionResponse {
            return TransactionResponse(
                id = transaction.id,
                transferId = transaction.transferId,
                accountId = transaction.accountId,
                amount = AmountDto.fromDomain(transaction.amount),
                valuationTimestamp = transaction.valuationTimestamp,
                purpose = transaction.purpose,
                type = transaction.type.name,
                sourceAccount = transaction.sourceAccount,
                destinationAccount = transaction.destinationAccount,
                createdAt = transaction.createdAt
            )
        }
    }
}
