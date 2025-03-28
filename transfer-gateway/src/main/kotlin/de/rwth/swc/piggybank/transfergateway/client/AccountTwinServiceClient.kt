package de.rwth.swc.piggybank.transfergateway.client

import de.rwth.swc.piggybank.transfergateway.dto.TransactionRequest

/**
 * Client interface for interacting with the AccountTwinService.
 * This abstraction simplifies testing by removing the need to mock complex WebClient chains.
 */
interface AccountTwinServiceClient {
    /**
     * Sends a transaction to the AccountTwinService.
     *
     * @param transactionRequest The transaction request to send
     */
    fun sendTransaction(transactionRequest: TransactionRequest)
}