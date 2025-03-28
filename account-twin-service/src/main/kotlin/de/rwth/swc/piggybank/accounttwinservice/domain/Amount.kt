package de.rwth.swc.piggybank.accounttwinservice.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.math.BigDecimal
import java.util.Currency

/**
 * Represents a monetary amount in the system.
 * An amount consists of the numerical value and the currency.
 *
 * @property value The numerical value of the amount
 * @property currency The currency of the amount
 * @throws IllegalArgumentException If the value is null or the currency is null
 */
@Embeddable
data class Amount(
    @Column(name = "amount_value")
    val value: BigDecimal,

    @Column(name = "currency_code")
    val currencyCode: String
) : Serializable {
    init {
        require(value != null) { "Amount value cannot be null" }
        require(currencyCode.isNotBlank()) { "Currency code cannot be blank" }
        // Validate that the currency code is valid
        Currency.getInstance(currencyCode)
    }

    /**
     * Gets the Currency object for this amount.
     */
    fun getCurrency(): Currency = Currency.getInstance(currencyCode)

    /**
     * Creates a new Amount with the same currency but a negated value.
     *
     * @return A new Amount with the negated value
     */
    fun negate(): Amount = Amount(value.negate(), currencyCode)

    /**
     * Adds another amount to this amount.
     * Both amounts must have the same currency.
     *
     * @param other The amount to add
     * @return A new Amount with the sum of the values
     * @throws IllegalArgumentException If the currencies are different
     */
    fun add(other: Amount): Amount {
        require(currencyCode == other.currencyCode) { "Cannot add amounts with different currencies" }
        return Amount(value.add(other.value), currencyCode)
    }

    /**
     * Checks if this amount is positive (greater than zero).
     *
     * @return true if the amount is positive, false otherwise
     */
    fun isPositive(): Boolean = value > BigDecimal.ZERO

    /**
     * Checks if this amount is negative (less than zero).
     *
     * @return true if the amount is negative, false otherwise
     */
    fun isNegative(): Boolean = value < BigDecimal.ZERO

    override fun toString(): String = "$value $currencyCode"

    companion object {
        /**
         * Creates an Amount from a string representation in the format "value currencyCode".
         *
         * @param amountString The string representation of the amount
         * @return The Amount object
         * @throws IllegalArgumentException If the string is not in the correct format
         */
        fun fromString(amountString: String): Amount {
            val parts = amountString.trim().split(" ")
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid amount string format: $amountString. Expected format: value currencyCode")
            }
            val value = try {
                BigDecimal(parts[0])
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid amount value: ${parts[0]}", e)
            }
            val currencyCode = parts[1]
            try {
                Currency.getInstance(currencyCode)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid currency code: $currencyCode", e)
            }
            return Amount(value, currencyCode)
        }
    }
}