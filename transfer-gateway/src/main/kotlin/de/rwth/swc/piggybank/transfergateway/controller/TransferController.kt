package de.rwth.swc.piggybank.transfergateway.controller

import de.rwth.swc.piggybank.transfergateway.dto.TransferRequest
import de.rwth.swc.piggybank.transfergateway.service.TransferService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory

/**
 * REST controller for handling transfer requests from external banking systems.
 */
@RestController
@RequestMapping("/api/transfers")
class TransferController(private val transferService: TransferService) {
    private val logger = LoggerFactory.getLogger(TransferController::class.java)

    /**
     * Handles a transfer request from an external banking system.
     *
     * @param transferRequest The transfer request
     * @return ResponseEntity with HTTP status 201 (Created) if the transfer was processed successfully,
     *         or HTTP status 500 (Internal Server Error) if an error occurred
     */
    @PostMapping
    fun handleTransfer(@Valid @RequestBody transferRequest: TransferRequest): ResponseEntity<Void> {
        logger.info("Received transfer request: {}", transferRequest)
        
        val transfer = transferRequest.toDomain()
        val success = transferService.processTransfer(transfer)
        
        return if (success) {
            logger.info("Transfer processed successfully")
            ResponseEntity.status(HttpStatus.CREATED).build()
        } else {
            logger.error("Failed to process transfer")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}