package de.rwth.swc.piggybank.accounttwinservice.dto

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import java.time.Instant

/**
 * Data Transfer Object for returning account information.
 *
 * @property id The unique identifier of the account
 * @property type The type of the account (e.g., BankAccount, PayPal)
 * @property identifier The identifier of the account (e.g., IBAN, email)
 * @property balance The current balance of the account
 * @property createdAt The timestamp when the account was created
 * @property transactions The list of transactions for the account (optional)
 */
data class AccountResponse(
    val id: String,
    val type: String,
    val identifier: String,
    val balance: AmountDto,
    val createdAt: Instant,
    val transactions: List<TransactionResponse>? = null
) {
    companion object {
        /**
         * Creates an AccountResponse from a domain Account object.
         *
         * @param account The domain Account object
         * @param includeTransactions Whether to include transactions in the response
         * @param transactions The list of transactions for the account (required if includeTransactions is true)
         * @return The AccountResponse
         */
        fun fromDomain(
            account: Account,
            includeTransactions: Boolean = false,
            transactions: List<TransactionResponse>? = null
        ): AccountResponse {
            return AccountResponse(
                id = account.id,
                type = account.type,
                identifier = account.identifier,
                balance = AmountDto.fromDomain(account.balance),
                createdAt = account.createdAt,
                transactions = if (includeTransactions) transactions else null
            )
        }
    }
}

/**
 * Data Transfer Object for returning just the account balance.
 *
 * @property id The unique identifier of the account
 * @property balance The current balance of the account
 */
data class AccountBalanceResponse(
    val id: String,
    val balance: AmountDto
) {
    companion object {
        /**
         * Creates an AccountBalanceResponse from a domain Account object.
         *
         * @param account The domain Account object
         * @return The AccountBalanceResponse
         */
        fun fromDomain(account: Account): AccountBalanceResponse {
            return AccountBalanceResponse(
                id = account.id,
                balance = AmountDto.fromDomain(account.balance)
            )
        }
    }
}