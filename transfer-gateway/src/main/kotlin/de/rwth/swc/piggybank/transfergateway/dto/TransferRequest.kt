package de.rwth.swc.piggybank.transfergateway.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.domain.Transfer
import java.time.Instant
import java.util.UUID
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * Data Transfer Object for receiving transfer requests from external banking systems.
 *
 * @property sourceAccount The source account of the transfer
 * @property targetAccount The target account of the transfer
 * @property amount The amount to transfer
 * @property valuationTimestamp The timestamp when the transfer was valued
 * @property purpose The purpose or description of the transfer
 */
data class TransferRequest @JsonCreator constructor(
    @JsonProperty("sourceAccount")
    @field:Valid
    @field:NotNull(message = "Source account is required")
    val sourceAccount: Account,

    @JsonProperty("targetAccount")
    @field:Valid
    @field:NotNull(message = "Target account is required")
    val targetAccount: Account,

    @JsonProperty("amount")
    @field:Valid
    @field:NotNull(message = "Amount is required")
    val amount: Amount,

    @JsonProperty("valuationTimestamp")
    @field:NotNull(message = "Valuation timestamp is required")
    val valuationTimestamp: Instant = Instant.now(),

    @JsonProperty("purpose")
    @field:NotBlank(message = "Purpose is required")
    val purpose: String
) {
    /**
     * Converts this DTO to a domain Transfer object.
     *
     * @return The domain Transfer object
     */
    fun toDomain(): Transfer {
        return Transfer(
            id = UUID.randomUUID(),
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            amount = amount,
            valuationTimestamp = valuationTimestamp,
            purpose = purpose
        )
    }
}
