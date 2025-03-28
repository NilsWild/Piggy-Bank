package de.rwth.swc.piggybank.accounttwinservice.domain

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
    DUMMY;

    /**
     * Determines if this transaction type increases the account balance.
     *
     * @return true if the transaction increases the balance, false otherwise
     */
    fun increasesBalance(): Boolean = this == CREDIT

    /**
     * Determines if this transaction type decreases the account balance.
     *
     * @return true if the transaction decreases the balance, false otherwise
     */
    fun decreasesBalance(): Boolean = this == DEBIT
}
