package de.rwth.swc.piggybank.transferclassifier.domain

/**
 * Represents an account in the system.
 * An account has a type and an identifier.
 *
 * @property type The type of the account (e.g., "IBAN", "CARD")
 * @property identifier The identifier of the account (e.g., the IBAN number)
 */
data class Account(
    val type: String,
    val identifier: String
)