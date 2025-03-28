package de.rwth.swc.piggybank.transfergateway.client

import de.rwth.swc.piggybank.transfergateway.dto.AccountTwinServiceTransactionRequest
import de.rwth.swc.piggybank.transfergateway.dto.TransactionRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * WebClient-based implementation of the AccountTwinServiceClient interface.
 * This class encapsulates all WebClient-related logic for interacting with the AccountTwinService.
 */
@Component
class WebClientAccountTwinServiceClient(
    private val webClientBuilder: WebClient.Builder,
    @Value("\${account-twin-service.url}") private val accountTwinServiceUrl: String
) : AccountTwinServiceClient {
    private val logger = LoggerFactory.getLogger(WebClientAccountTwinServiceClient::class.java)
    private val webClient by lazy {
        webClientBuilder.baseUrl(accountTwinServiceUrl).build()
    }

    /**
     * Sends a transaction to the AccountTwinService using WebClient.
     *
     * @param transactionRequest The transaction request to send
     */
    override fun sendTransaction(transactionRequest: TransactionRequest) {
        logger.info("Sending transaction to AccountTwinService: {}", transactionRequest)
        try {
            // Convert TransactionRequest to AccountTwinServiceTransactionRequest
            val accountTwinServiceRequest = AccountTwinServiceTransactionRequest.fromTransactionRequest(transactionRequest)
            logger.debug("Converted to AccountTwinServiceTransactionRequest: {}", accountTwinServiceRequest)

            webClient.post()
                .uri("/api/transactions")
                .bodyValue(accountTwinServiceRequest)
                .retrieve()
                .bodyToMono(Void::class.java)
                .block()
            logger.info("Transaction sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send transaction to AccountTwinService", e)
            throw e
        }
    }
}
