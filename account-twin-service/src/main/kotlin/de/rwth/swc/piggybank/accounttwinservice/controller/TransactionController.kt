package de.rwth.swc.piggybank.accounttwinservice.controller

import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionResponse
import de.rwth.swc.piggybank.accounttwinservice.service.TransactionService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * REST controller for managing transactions.
 */
@RestController
@RequestMapping("/api/transactions")
class TransactionController(private val transactionService: TransactionService) {
    private val logger = LoggerFactory.getLogger(TransactionController::class.java)

    /**
     * Processes a transaction.
     *
     * @param transactionRequest The transaction request
     * @return The processed transaction
     */
    @PostMapping
    fun processTransaction(@Valid @RequestBody transactionRequest: TransactionRequest): ResponseEntity<TransactionResponse> {
        logger.info("Processing transaction: {}", transactionRequest)

        try {
            // Check if the transaction already exists
            if (transactionService.transactionExists(transactionRequest.transferId, transactionRequest.accountId)) {
                logger.info("Transaction already exists, skipping")
                return ResponseEntity.status(HttpStatus.OK)
                    .body(TransactionResponse.fromDomain(
                        transactionService.getTransactionByTransferIdAndAccountId(
                            transactionRequest.transferId,
                            transactionRequest.accountId
                        )!!
                    ))
            }

            // Process the transaction
            val transaction = transactionService.processTransaction(transactionRequest)
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransactionResponse.fromDomain(transaction))
        } catch (e: IllegalStateException) {
            logger.error("Failed to process transaction", e)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: Exception) {
            logger.error("Failed to process transaction", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process transaction")
        }
    }

    /**
     * Gets a transaction by its ID.
     *
     * @param transactionId The ID of the transaction
     * @return The transaction
     */
    @GetMapping("/{transactionId}")
    fun getTransaction(@PathVariable transactionId: UUID): ResponseEntity<TransactionResponse> {
        logger.info("Getting transaction with ID: {}", transactionId)

        val transaction = transactionService.getTransaction(transactionId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found")

        return ResponseEntity.ok(TransactionResponse.fromDomain(transaction))
    }

    /**
     * Gets all transactions for an account.
     *
     * @param accountId The ID of the account
     * @param pageable The pagination information
     * @return A page of transactions
     */
    @GetMapping("/by-account/{accountId}")
    fun getTransactionsByAccount(
        @PathVariable accountId: String,
        pageable: Pageable
    ): ResponseEntity<Page<TransactionResponse>> {
        logger.info("Getting transactions for account with ID: {}", accountId)

        val transactions = transactionService.getTransactionsByAccount(accountId, pageable)
        val transactionResponses = transactions.map { TransactionResponse.fromDomain(it) }

        return ResponseEntity.ok(transactionResponses)
    }
}
