package de.rwth.swc.piggybank.transfergateway.domain

import java.time.Instant
import java.util.*

/**
 * Represents a transfer between two accounts in the system.
 * A transfer consists of the source account, the target account, an amount, a valuation timestamp, and a purpose.
 *
 * @property id The unique identifier of the transfer
 * @property sourceAccount The account from which the money is transferred
 * @property targetAccount The account to which the money is transferred
 * @property amount The amount of money transferred
 * @property valuationTimestamp The timestamp when the transfer was valued
 * @property purpose The purpose or description of the transfer
 */
data class Transfer(
    val id: UUID = UUID.randomUUID(),
    val sourceAccount: Account,
    val targetAccount: Account,
    val amount: Amount,
    val valuationTimestamp: Instant,
    val purpose: String
) {
    /**
     * Checks if this transfer involves the specified account (either as source or target).
     *
     * @param account The account to check
     * @return true if the transfer involves the account, false otherwise
     */
    fun involvesAccount(account: Account): Boolean =
        sourceAccount == account || targetAccount == account

}
