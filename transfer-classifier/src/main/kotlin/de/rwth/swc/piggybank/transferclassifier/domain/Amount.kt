package de.rwth.swc.piggybank.transferclassifier.domain

import java.io.Serializable
import java.math.BigDecimal
import java.util.Currency

/**
 * Represents a monetary amount in the system.
 * An amount consists of the numerical value and the currency.
 *
 * @property value The numerical value of the amount
 * @property currencyCode The currency code of the amount (e.g., USD, EUR)
 */
data class Amount(
    val value: BigDecimal,
    val currencyCode: String
) : Serializable {
    override fun toString(): String = "$value $currencyCode"
}