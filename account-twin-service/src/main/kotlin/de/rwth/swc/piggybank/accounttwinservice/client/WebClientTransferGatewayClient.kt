package de.rwth.swc.piggybank.accounttwinservice.client

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * WebClient-based implementation of the TransferGatewayClient interface.
 * This class encapsulates all WebClient-related logic for interacting with the TransferGateway.
 */
@Component
class WebClientTransferGatewayClient(
    private val webClientBuilder: WebClient.Builder,
    @Value("\${transfer-gateway.url}") private val transferGatewayUrl: String
) : TransferGatewayClient {
    private val logger = LoggerFactory.getLogger(WebClientTransferGatewayClient::class.java)
    private val webClient by lazy {
        webClientBuilder.baseUrl(transferGatewayUrl).build()
    }

    /**
     * Adds an account to the monitored accounts in the TransferGateway using WebClient.
     *
     * @param account The account to add
     * @return true if the account was added, false if it was already in the list
     */
    override fun addMonitoredAccount(account: Account): Boolean {
        logger.info("Adding account to monitored accounts in TransferGateway: {}", account)

        // Create a request object with the account data
        val accountRequest = mapToTransferGatewayAccount(account)

        try {
            val response = webClient.post()
                .uri("/api/accounts")
                .bodyValue(accountRequest)
                .retrieve()
                .toBodilessEntity()
                .block()

            // If we get a 201 CREATED response, the account was added successfully
            val added = response?.statusCode == HttpStatus.CREATED
            logger.info("Account {} added to monitored accounts in TransferGateway", if (added) "successfully" else "already exists")
            return added
        } catch (e: WebClientResponseException) {
            // If we get a 409 CONFLICT response, the account was already in the list
            if (e.statusCode == HttpStatus.CONFLICT) {
                logger.info("Account already exists in monitored accounts in TransferGateway")
                return false
            }

            // For any other error, log and rethrow
            logger.error("Failed to add account to monitored accounts in TransferGateway", e)
            throw e
        } catch (e: Exception) {
            logger.error("Failed to add account to monitored accounts in TransferGateway", e)
            throw e
        }
    }

    /**
     * Maps an account from the account-twin-service domain to a request object for the TransferGateway.
     *
     * @param account The account to map
     * @return The mapped account request
     */
    private fun mapToTransferGatewayAccount(account: Account): Map<String, Any> {
        // Create a TransferGateway Account object with the type, identifier, and accountId
        val transferGatewayAccount = mapOf(
            "type" to account.type,
            "identifier" to account.identifier,
            "accountId" to account.id
        )

        // Wrap it in an AccountRequest object
        return mapOf("account" to transferGatewayAccount)
    }
}
