package de.rwth.swc.piggybank.transfergateway.domain

import java.io.Serializable

/**
 * Represents an account in the system.
 * An account is identified by a type (e.g., BankAccount or PayPal) and an identifier (e.g., IBAN or some other ID).
 *
 * @property type The type of the account (e.g., BankAccount, PayPal)
 * @property identifier The identifier of the account (e.g., IBAN, email)
 * @property accountId The unique identifier of the account in the account-twin-service (optional)
 * @throws IllegalArgumentException If the type or identifier is blank
 */
data class Account(
    val type: String,
    val identifier: String,
    val accountId: String? = null
) : Serializable {
    init {
        require(type.isNotBlank()) { "Account type cannot be blank" }
        require(identifier.isNotBlank()) { "Account identifier cannot be blank" }
    }
    override fun toString(): String = "$type:$identifier"

    companion object {
        /**
         * Creates an Account from a string representation in the format "type:identifier".
         *
         * @param accountString The string representation of the account
         * @return The Account object
         * @throws IllegalArgumentException If the string is not in the correct format
         */
        fun fromString(accountString: String): Account {
            val parts = accountString.split(":")
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid account string format: $accountString. Expected format: type:identifier")
            }
            return Account(parts[0], parts[1])
        }
    }
}
