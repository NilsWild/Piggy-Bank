package de.rwth.swc.piggybank.goalservice.service

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for storing transfer classification information.
 * This is used to correlate transfer IDs with account IDs and transaction information.
 */
@Component
class TransferClassificationCache {
    // Map of transfer ID to account ID
    private val transferAccountMap = ConcurrentHashMap<UUID, String>()
    
    // Map of transfer ID to transaction amount
    private val transferAmountMap = ConcurrentHashMap<UUID, String>()
    
    // Map of transfer ID to transaction type
    private val transferTypeMap = ConcurrentHashMap<UUID, String>()
    
    // Map of transfer ID to transaction purpose
    private val transferPurposeMap = ConcurrentHashMap<UUID, String>()

    /**
     * Stores transfer information in the cache.
     *
     * @param transferId The ID of the transfer
     * @param accountId The ID of the account
     * @param amount The amount of the transfer
     * @param type The type of the transfer
     * @param purpose The purpose of the transfer
     */
    fun storeTransferInfo(
        transferId: UUID,
        accountId: String,
        amount: String,
        type: String,
        purpose: String
    ) {
        transferAccountMap[transferId] = accountId
        transferAmountMap[transferId] = amount
        transferTypeMap[transferId] = type
        transferPurposeMap[transferId] = purpose
    }

    /**
     * Gets the account ID for a transfer.
     *
     * @param transferId The ID of the transfer
     * @return The account ID, or null if not found
     */
    fun getAccountId(transferId: UUID): String? {
        return transferAccountMap[transferId]
    }

    /**
     * Gets the amount for a transfer.
     *
     * @param transferId The ID of the transfer
     * @return The amount, or null if not found
     */
    fun getAmount(transferId: UUID): String? {
        return transferAmountMap[transferId]
    }

    /**
     * Gets the type for a transfer.
     *
     * @param transferId The ID of the transfer
     * @return The type, or null if not found
     */
    fun getType(transferId: UUID): String? {
        return transferTypeMap[transferId]
    }

    /**
     * Gets the purpose for a transfer.
     *
     * @param transferId The ID of the transfer
     * @return The purpose, or null if not found
     */
    fun getPurpose(transferId: UUID): String? {
        return transferPurposeMap[transferId]
    }

    /**
     * Removes transfer information from the cache.
     *
     * @param transferId The ID of the transfer
     */
    fun removeTransferInfo(transferId: UUID) {
        transferAccountMap.remove(transferId)
        transferAmountMap.remove(transferId)
        transferTypeMap.remove(transferId)
        transferPurposeMap.remove(transferId)
    }
}