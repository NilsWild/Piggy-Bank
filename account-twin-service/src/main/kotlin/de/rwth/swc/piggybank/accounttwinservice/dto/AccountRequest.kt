package de.rwth.swc.piggybank.accounttwinservice.dto

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

/**
 * Data Transfer Object for creating or updating an account.
 *
 * @property type The type of the account (e.g., BankAccount, PayPal)
 * @property identifier The identifier of the account (e.g., IBAN, email)
 * @property initialBalance The initial balance of the account
 */
data class AccountRequest(
    @field:NotBlank(message = "Account type is required")
    val type: String,

    @field:NotBlank(message = "Account identifier is required")
    val identifier: String,

    @field:Valid
    @field:NotNull(message = "Initial balance is required")
    val initialBalance: AmountDto
) {
    /**
     * Converts this DTO to a domain Account object.
     *
     * @return The domain Account object
     */
    fun toDomain(): Account {
        val amount = Amount(initialBalance.value, initialBalance.currencyCode)
        return Account.create(type, identifier, amount)
    }
}

/**
 * Data Transfer Object for an amount.
 *
 * @property value The numerical value of the amount
 * @property currencyCode The currency code of the amount (e.g., USD, EUR)
 */
data class AmountDto(
    @field:NotNull(message = "Amount value is required")
    val value: BigDecimal,

    @field:NotBlank(message = "Currency code is required")
    val currencyCode: String
) {
    /**
     * Converts this DTO to a domain Amount object.
     *
     * @return The domain Amount object
     */
    fun toDomain(): Amount {
        return Amount(value, currencyCode)
    }

    companion object {
        /**
         * Creates an AmountDto from a domain Amount object.
         *
         * @param amount The domain Amount object
         * @return The AmountDto
         */
        fun fromDomain(amount: Amount): AmountDto {
            return AmountDto(amount.value, amount.currencyCode)
        }
    }
}