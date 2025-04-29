package de.rwth.swc.piggybank.accounttwinservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Clock
import java.time.Instant

/**
 * Represents a monitored account in the system.
 * An account is identified by a type (e.g., BankAccount or PayPal) and an identifier (e.g., IBAN or some other ID).
 * It also includes the current balance and the creation timestamp.
 *
 * @property id The unique identifier of the account in the format "type:identifier"
 * @property type The type of the account (e.g., BankAccount, PayPal)
 * @property identifier The identifier of the account (e.g., IBAN, email)
 * @property balance The current balance of the account
 * @property createdAt The timestamp when the account was created
 * @throws IllegalArgumentException If the type or identifier is blank
 */
@Entity
@Table(name = "accounts")
data class Account(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Column(name = "type", nullable = false)
    val type: String,

    @Column(name = "identifier", nullable = false)
    val identifier: String,

    @Embedded
    val balance: Amount,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) : Serializable {
    init {
        require(type.isNotBlank()) { "Account type cannot be blank" }
        require(identifier.isNotBlank()) { "Account identifier cannot be blank" }
    }

    /**
     * Updates the account balance with a transaction.
     *
     * @param amount The amount of the transaction
     * @param type The type of the transaction (CREDIT or DEBIT)
     * @return A new Account with the updated balance
     */
    fun updateBalance(amount: Amount, type: TransactionType): Account {
        val newBalance = if (type.increasesBalance()) {
            balance.add(amount)
        } else {
            balance.add(amount.negate())
        }
        return copy(balance = newBalance)
    }

}
