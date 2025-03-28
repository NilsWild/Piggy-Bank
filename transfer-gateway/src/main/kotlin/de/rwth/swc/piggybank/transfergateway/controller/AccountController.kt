package de.rwth.swc.piggybank.transfergateway.controller

import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.dto.AccountRequest
import de.rwth.swc.piggybank.transfergateway.service.AccountService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory

/**
 * REST controller for managing monitored accounts.
 */
@RestController
@RequestMapping("/api/accounts")
class AccountController(private val accountService: AccountService) {
    private val logger = LoggerFactory.getLogger(AccountController::class.java)

    /**
     * Gets all monitored accounts.
     *
     * @return ResponseEntity with a set of all monitored accounts and HTTP status 200 (OK)
     */
    @GetMapping
    fun getAllMonitoredAccounts(): ResponseEntity<Set<Account>> {
        logger.info("Getting all monitored accounts")
        val accounts = accountService.getAllMonitoredAccounts()
        return ResponseEntity.ok(accounts)
    }

    /**
     * Adds an account to the list of monitored accounts.
     *
     * @param accountRequest The account request
     * @return ResponseEntity with HTTP status 201 (Created) if the account was added,
     *         or HTTP status 409 (Conflict) if the account was already in the list
     */
    @PostMapping
    fun addMonitoredAccount(@Valid @RequestBody accountRequest: AccountRequest): ResponseEntity<Void> {
        logger.info("Adding monitored account: {}", accountRequest)
        
        val account = accountRequest.toDomain()
        val added = accountService.addMonitoredAccount(account)
        
        return if (added) {
            logger.info("Account added to monitored accounts")
            ResponseEntity.status(HttpStatus.CREATED).build()
        } else {
            logger.info("Account already in monitored accounts")
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    /**
     * Removes an account from the list of monitored accounts.
     *
     * @param accountRequest The account request
     * @return ResponseEntity with HTTP status 204 (No Content) if the account was removed,
     *         or HTTP status 404 (Not Found) if the account was not in the list
     */
    @DeleteMapping
    fun removeMonitoredAccount(@Valid @RequestBody accountRequest: AccountRequest): ResponseEntity<Void> {
        logger.info("Removing monitored account: {}", accountRequest)
        
        val account = accountRequest.toDomain()
        val removed = accountService.removeMonitoredAccount(account)
        
        return if (removed) {
            logger.info("Account removed from monitored accounts")
            ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        } else {
            logger.info("Account not found in monitored accounts")
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }
}