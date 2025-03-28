package de.rwth.swc.piggybank.accounttwinservice.repository

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for Account entities.
 */
@Repository
interface AccountRepository : JpaRepository<Account, String> {
    /**
     * Finds an account by its type and identifier.
     *
     * @param type The type of the account
     * @param identifier The identifier of the account
     * @return The account, or null if not found
     */
    fun findByTypeAndIdentifier(type: String, identifier: String): Account?

    /**
     * Checks if an account exists by its type and identifier.
     *
     * @param type The type of the account
     * @param identifier The identifier of the account
     * @return true if the account exists, false otherwise
     */
    fun existsByTypeAndIdentifier(type: String, identifier: String): Boolean
}