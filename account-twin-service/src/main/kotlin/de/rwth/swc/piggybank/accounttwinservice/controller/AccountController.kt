package de.rwth.swc.piggybank.accounttwinservice.controller

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountBalanceResponse
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountResponse
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionResponse
import de.rwth.swc.piggybank.accounttwinservice.service.AccountService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * REST controller for managing accounts.
 */
@RestController
@RequestMapping("/api/accounts")
class AccountController(private val accountService: AccountService) {
    private val logger = LoggerFactory.getLogger(AccountController::class.java)

    /**
     * Creates a new monitored account.
     *
     * @param accountRequest The account request
     * @return The created account
     */
    @PostMapping
    fun createAccount(@Valid @RequestBody accountRequest: AccountRequest): ResponseEntity<AccountResponse> {
        logger.info("Creating account: {}", accountRequest)
        
        try {
            val account = accountRequest.toDomain()
            val createdAccount = accountService.createAccount(account)
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(AccountResponse.fromDomain(createdAccount))
        } catch (e: IllegalStateException) {
            logger.error("Failed to create account", e)
            throw ResponseStatusException(HttpStatus.CONFLICT, e.message)
        } catch (e: Exception) {
            logger.error("Failed to create account", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create account")
        }
    }

    /**
     * Gets an account by its ID.
     *
     * @param accountId The ID of the account
     * @param includeTransactions Whether to include transactions in the response
     * @return The account
     */
    @GetMapping("/{accountId}")
    fun getAccount(
        @PathVariable accountId: String,
        @RequestParam(defaultValue = "false") includeTransactions: Boolean
    ): ResponseEntity<AccountResponse> {
        logger.info("Getting account with ID: {}", accountId)
        
        val account = accountService.getAccount(accountId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
        
        return if (includeTransactions) {
            val transactions = accountService.getAccountTransactions(accountId, Pageable.unpaged())
            val transactionResponses = transactions.map { TransactionResponse.fromDomain(it) }.content
            ResponseEntity.ok(AccountResponse.fromDomain(account, true, transactionResponses))
        } else {
            ResponseEntity.ok(AccountResponse.fromDomain(account))
        }
    }

    /**
     * Gets an account by its type and identifier.
     *
     * @param type The type of the account
     * @param identifier The identifier of the account
     * @param includeTransactions Whether to include transactions in the response
     * @return The account
     */
    @GetMapping("/by-type-and-identifier")
    fun getAccountByTypeAndIdentifier(
        @RequestParam type: String,
        @RequestParam identifier: String,
        @RequestParam(defaultValue = "false") includeTransactions: Boolean
    ): ResponseEntity<AccountResponse> {
        logger.info("Getting account with type: {} and identifier: {}", type, identifier)
        
        val account = accountService.getAccountByTypeAndIdentifier(type, identifier)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
        
        return if (includeTransactions) {
            val transactions = accountService.getAccountTransactions(account.id, Pageable.unpaged())
            val transactionResponses = transactions.map { TransactionResponse.fromDomain(it) }.content
            ResponseEntity.ok(AccountResponse.fromDomain(account, true, transactionResponses))
        } else {
            ResponseEntity.ok(AccountResponse.fromDomain(account))
        }
    }

    /**
     * Gets all accounts.
     *
     * @return A list of all accounts
     */
    @GetMapping
    fun getAllAccounts(): ResponseEntity<List<AccountResponse>> {
        logger.info("Getting all accounts")
        
        val accounts = accountService.getAllAccounts()
        val accountResponses = accounts.map { AccountResponse.fromDomain(it) }
        
        return ResponseEntity.ok(accountResponses)
    }

    /**
     * Gets the balance of an account.
     *
     * @param accountId The ID of the account
     * @return The account balance
     */
    @GetMapping("/{accountId}/balance")
    fun getAccountBalance(@PathVariable accountId: String): ResponseEntity<AccountBalanceResponse> {
        logger.info("Getting balance for account with ID: {}", accountId)
        
        val account = accountService.getAccount(accountId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
        
        return ResponseEntity.ok(AccountBalanceResponse.fromDomain(account))
    }

    /**
     * Deletes an account by its ID.
     *
     * @param accountId The ID of the account
     * @return No content
     */
    @DeleteMapping("/{accountId}")
    fun deleteAccount(@PathVariable accountId: String): ResponseEntity<Void> {
        logger.info("Deleting account with ID: {}", accountId)
        
        val deleted = accountService.deleteAccount(accountId)
        
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
        }
    }
}