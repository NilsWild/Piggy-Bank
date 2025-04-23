package de.rwth.swc.piggybank.accounttwinservice.dto

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import java.io.Serializable
import java.util.UUID

/**
 * Sealed class hierarchy for account events.
 * This is used to ensure type safety and proper serialization of events sent to RabbitMQ.
 */
sealed class AccountEvent : Serializable {
    abstract val eventType: String
    abstract val accountId: String
    abstract val accountType: String
    abstract val accountIdentifier: String
}

/**
 * Event sent when an account is created.
 *
 * @property eventType The type of the event (always "ACCOUNT_CREATED")
 * @property accountId The ID of the account
 * @property accountType The type of the account
 * @property accountIdentifier The identifier of the account
 * @property value The value of the account balance
 * @property currencyCode The currency code of the account balance
 */
data class AccountCreatedEvent(
    override val eventType: String = "ACCOUNT_CREATED",
    override val accountId: String,
    override val accountType: String,
    override val accountIdentifier: String,
    val value: String,
    val currencyCode: String
) : AccountEvent() {
    companion object {
        /**
         * Creates an AccountCreatedEvent from an Account.
         *
         * @param account The account that was created
         * @return The AccountCreatedEvent
         */
        fun fromDomain(account: Account): AccountCreatedEvent {
            return AccountCreatedEvent(
                accountId = account.id,
                accountType = account.type,
                accountIdentifier = account.identifier,
                value = account.balance.value.toString(),
                currencyCode = account.balance.currencyCode
            )
        }
    }
}

/**
 * Event sent when an account is updated.
 *
 * @property eventType The type of the event (always "ACCOUNT_UPDATED")
 * @property accountId The ID of the account
 * @property accountType The type of the account
 * @property accountIdentifier The identifier of the account
 * @property value The value of the account balance
 * @property currencyCode The currency code of the account balance
 * @property transactionId The ID of the transaction that caused the update
 * @property transactionAmount The amount of the transaction
 * @property transactionType The type of the transaction
 * @property transactionPurpose The purpose of the transaction
 */
data class AccountUpdatedEvent(
    override val eventType: String = "ACCOUNT_UPDATED",
    override val accountId: String,
    override val accountType: String,
    override val accountIdentifier: String,
    val value: String,
    val currencyCode: String,
    val transactionId: String,
    val transferId: String,
    val transactionAmount: TransactionAmountDto,
    val transactionType: String,
    val transactionPurpose: String
) : AccountEvent() {
    companion object {
        /**
         * Creates an AccountUpdatedEvent from an Account and a Transaction.
         *
         * @param account The account that was updated
         * @param transaction The transaction that caused the update
         * @return The AccountUpdatedEvent
         */
        fun fromDomain(account: Account, transaction: Transaction): AccountUpdatedEvent {
            return AccountUpdatedEvent(
                accountId = account.id,
                accountType = account.type,
                accountIdentifier = account.identifier,
                value = account.balance.value.toString(),
                currencyCode = account.balance.currencyCode,
                transactionId = transaction.id.toString(),
                transferId = transaction.transferId.toString(),
                transactionAmount = TransactionAmountDto(
                    value = transaction.amount.value.toString(),
                    currencyCode = transaction.amount.currencyCode
                ),
                transactionType = transaction.type.name,
                transactionPurpose = transaction.purpose
            )
        }
    }
}

/**
 * Event sent when an account is deleted.
 *
 * @property eventType The type of the event (always "ACCOUNT_DELETED")
 * @property accountId The ID of the account
 * @property accountType The type of the account
 * @property accountIdentifier The identifier of the account
 */
data class AccountDeletedEvent(
    override val eventType: String = "ACCOUNT_DELETED",
    override val accountId: String,
    override val accountType: String,
    override val accountIdentifier: String
) : AccountEvent() {
    companion object {
        /**
         * Creates an AccountDeletedEvent from an Account.
         *
         * @param account The account that was deleted
         * @return The AccountDeletedEvent
         */
        fun fromDomain(account: Account): AccountDeletedEvent {
            return AccountDeletedEvent(
                accountId = account.id,
                accountType = account.type,
                accountIdentifier = account.identifier
            )
        }
    }
}

/**
 * Data Transfer Object for a transaction amount.
 * This is used in the AccountUpdatedEvent to ensure proper serialization.
 *
 * @property value The value of the transaction amount
 * @property currencyCode The currency code of the transaction amount
 */
data class TransactionAmountDto(
    val value: String,
    val currencyCode: String
) : Serializable
