package de.rwth.swc.piggybank.accounttwinservice.dto

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.domain.TransactionType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object for creating a transaction.
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
 */
data class TransactionRequest(
    val id: UUID? = null,
    val transferId: UUID,
    @field:NotBlank(message = "Affected account ID is required")
    val affectedAccountId: String,
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
            id = id ?: UUID.randomUUID(),
            transferId = transferId,
            affectedAccountId = affectedAccountId,
            account = account,
            amount = amount.toDomain(),
            valuationTimestamp = valuationTimestamp,
            purpose = purpose,
            type = TransactionType.valueOf(type),
            sourceAccount = sourceAccount,
            destinationAccount = destinationAccount
        )
    }

    /**
     * Converts this DTO to a domain Transaction object with controlled time and ID generation.
     *
     * @param account The domain Account object
     * @param clock The clock to use for getting the creation time
     * @param id The unique identifier of the transaction (optional)
     * @return The domain Transaction object
     */
    fun toDomain(account: Account, clock: Clock, id: UUID = UUID.randomUUID()): Transaction {
        return Transaction(
            id = id,
            transferId = transferId,
            affectedAccountId = affectedAccountId,
            account = account,
            amount = amount.toDomain(),
            valuationTimestamp = valuationTimestamp,
            purpose = purpose,
            type = TransactionType.valueOf(type),
            sourceAccount = sourceAccount,
            destinationAccount = destinationAccount,
            createdAt = Instant.now(clock)
        )
    }
}

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
                affectedAccountId = transaction.affectedAccountId,
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
