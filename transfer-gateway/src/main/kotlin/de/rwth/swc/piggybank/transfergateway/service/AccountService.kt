package de.rwth.swc.piggybank.transfergateway.service

import de.rwth.swc.piggybank.transfergateway.domain.Account
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing monitored accounts.
 */
@Service
class AccountService {
    private val monitoredAccounts = ConcurrentHashMap.newKeySet<Account>()

    /**
     * Adds an account to the list of monitored accounts.
     *
     * @param account The account to add
     * @return true if the account was added, false if it was already in the list
     */
    fun addMonitoredAccount(account: Account): Boolean = monitoredAccounts.add(account)

    /**
     * Removes an account from the list of monitored accounts.
     *
     * @param account The account to remove
     * @return true if the account was removed, false if it was not in the list
     */
    fun removeMonitoredAccount(account: Account): Boolean = monitoredAccounts.remove(account)

    /**
     * Checks if an account is monitored.
     *
     * @param account The account to check
     * @return true if the account is monitored, false otherwise
     */
    fun isMonitored(account: Account): Boolean = monitoredAccounts.any { it.type == account.type && it.identifier == account.identifier }

    /**
     * Gets all monitored accounts.
     *
     * @return A set of all monitored accounts
     */
    fun getAllMonitoredAccounts(): Set<Account> = monitoredAccounts.toSet()
}
