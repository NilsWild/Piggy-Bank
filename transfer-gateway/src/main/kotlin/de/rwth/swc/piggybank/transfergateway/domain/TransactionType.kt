package de.rwth.swc.piggybank.transfergateway.domain

/**
 * Represents the type of a transaction.
 * A transaction can be either a CREDIT (money coming in), a DEBIT (money going out), or a DUMMY (initial balance).
 */
enum class TransactionType {
    /**
     * Represents a credit transaction (money coming in).
     */
    CREDIT,

    /**
     * Represents a debit transaction (money going out).
     */
    DEBIT,

    /**
     * Represents a dummy transaction (initial balance).
     */
    DUMMY
}
