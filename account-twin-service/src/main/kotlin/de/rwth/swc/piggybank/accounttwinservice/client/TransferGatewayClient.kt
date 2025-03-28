package de.rwth.swc.piggybank.accounttwinservice.client

import de.rwth.swc.piggybank.accounttwinservice.domain.Account

/**
 * Client interface for interacting with the TransferGateway.
 * This abstraction simplifies testing by removing the need to mock complex WebClient chains.
 */
interface TransferGatewayClient {
    /**
     * Adds an account to the monitored accounts in the TransferGateway.
     *
     * @param account The account to add
     * @return true if the account was added, false if it was already in the list
     */
    fun addMonitoredAccount(account: Account): Boolean
}