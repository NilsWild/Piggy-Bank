package de.rwth.swc.piggybank.transferclassifier.domain

import java.time.Clock
import java.time.Instant
import java.util.UUID

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
}
