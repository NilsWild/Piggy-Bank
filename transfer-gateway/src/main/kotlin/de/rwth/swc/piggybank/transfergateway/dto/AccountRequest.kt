package de.rwth.swc.piggybank.transfergateway.dto

import de.rwth.swc.piggybank.transfergateway.domain.Account
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

/**
 * Data Transfer Object for receiving account creation/update requests.
 *
 * @property account The account to create or update
 */
data class AccountRequest(
    @field:Valid
    @field:NotNull(message = "Account is required")
    val account: Account
) {
    /**
     * Converts this DTO to a domain Account object.
     *
     * @return The domain Account object
     */
    fun toDomain(): Account = account
}
