package de.rwth.swc.piggybank.goalservice.service

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for storing transfer classification information.
 * This is used to correlate transfer IDs with account IDs and transaction information.
 * It also stores classifications for transfers and can handle events arriving in any order.
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

    // Map of transaction ID to transfer ID
    private val transactionToTransferMap = ConcurrentHashMap<UUID, UUID>()

    // Map of transfer ID to classifications
    private val transferClassificationsMap = ConcurrentHashMap<UUID, List<String>>()

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
     * Stores classification information in the cache.
     *
     * @param transferId The ID of the transfer
     * @param classifications The classifications for the transfer
     */
    fun storeClassifications(
        transferId: UUID,
        classifications: List<String>
    ) {
        transferClassificationsMap[transferId] = classifications
    }

    /**
     * Gets the account ID for a transfer.
     *
     * @param transferId The ID of the transfer or transaction
     * @return The account ID, or null if not found
     */
    fun getAccountId(transferId: UUID): String? {
        // Try to get the account ID directly using the transfer ID
        val accountId = transferAccountMap[transferId]
        if (accountId != null) {
            return accountId
        }

        // If not found, check if this is a transaction ID that maps to a transfer ID
        val mappedTransferId = transactionToTransferMap[transferId]
        if (mappedTransferId != null) {
            return transferAccountMap[mappedTransferId]
        }

        return null
    }

    /**
     * Gets the amount for a transfer.
     *
     * @param transferId The ID of the transfer or transaction
     * @return The amount, or null if not found
     */
    fun getAmount(transferId: UUID): String? {
        // Try to get the amount directly using the transfer ID
        val amount = transferAmountMap[transferId]
        if (amount != null) {
            return amount
        }

        // If not found, check if this is a transaction ID that maps to a transfer ID
        val mappedTransferId = transactionToTransferMap[transferId]
        if (mappedTransferId != null) {
            return transferAmountMap[mappedTransferId]
        }

        return null
    }

    /**
     * Gets the type for a transfer.
     *
     * @param transferId The ID of the transfer or transaction
     * @return The type, or null if not found
     */
    fun getType(transferId: UUID): String? {
        // Try to get the type directly using the transfer ID
        val type = transferTypeMap[transferId]
        if (type != null) {
            return type
        }

        // If not found, check if this is a transaction ID that maps to a transfer ID
        val mappedTransferId = transactionToTransferMap[transferId]
        if (mappedTransferId != null) {
            return transferTypeMap[mappedTransferId]
        }

        return null
    }

    /**
     * Gets the purpose for a transfer.
     *
     * @param transferId The ID of the transfer or transaction
     * @return The purpose, or null if not found
     */
    fun getPurpose(transferId: UUID): String? {
        // Try to get the purpose directly using the transfer ID
        val purpose = transferPurposeMap[transferId]
        if (purpose != null) {
            return purpose
        }

        // If not found, check if this is a transaction ID that maps to a transfer ID
        val mappedTransferId = transactionToTransferMap[transferId]
        if (mappedTransferId != null) {
            return transferPurposeMap[mappedTransferId]
        }

        return null
    }

    /**
     * Gets the classifications for a transfer.
     *
     * @param transferId The ID of the transfer or transaction
     * @return The classifications, or null if not found
     */
    fun getClassifications(transferId: UUID): List<String>? {
        // Try to get the classifications directly using the transfer ID
        val classifications = transferClassificationsMap[transferId]
        if (classifications != null) {
            return classifications
        }

        // If not found, check if this is a transaction ID that maps to a transfer ID
        val mappedTransferId = transactionToTransferMap[transferId]
        if (mappedTransferId != null) {
            return transferClassificationsMap[mappedTransferId]
        }

        return null
    }

    /**
     * Checks if transfer information is available for a transfer.
     *
     * @param transferId The ID of the transfer or transaction
     * @return true if transfer information is available, false otherwise
     */
    fun hasTransferInfo(transferId: UUID): Boolean {
        val actualTransferId = if (transactionToTransferMap.containsKey(transferId)) {
            transactionToTransferMap[transferId]
        } else {
            transferId
        }

        return actualTransferId != null && 
               transferAccountMap.containsKey(actualTransferId) && 
               transferAmountMap.containsKey(actualTransferId) && 
               transferTypeMap.containsKey(actualTransferId) && 
               transferPurposeMap.containsKey(actualTransferId)
    }

    /**
     * Checks if classifications are available for a transfer.
     *
     * @param transferId The ID of the transfer or transaction
     * @return true if classifications are available, false otherwise
     */
    fun hasClassifications(transferId: UUID): Boolean {
        val actualTransferId = if (transactionToTransferMap.containsKey(transferId)) {
            transactionToTransferMap[transferId]
        } else {
            transferId
        }

        return actualTransferId != null && transferClassificationsMap.containsKey(actualTransferId)
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
        transferClassificationsMap.remove(transferId)

        // Remove any transaction-to-transfer mappings for this transfer ID
        val transactionIdsToRemove = mutableListOf<UUID>()
        for ((transactionId, mappedTransferId) in transactionToTransferMap) {
            if (mappedTransferId == transferId) {
                transactionIdsToRemove.add(transactionId)
            }
        }

        for (transactionId in transactionIdsToRemove) {
            transactionToTransferMap.remove(transactionId)
        }
    }

    /**
     * Maps a transaction ID to a transfer ID.
     * This is used when we receive a transfer event and want to associate it with a transaction.
     *
     * @param transactionId The ID of the transaction
     * @param transferId The ID of the transfer
     */
    fun mapTransactionToTransfer(transactionId: UUID, transferId: UUID) {
        transactionToTransferMap[transactionId] = transferId
    }

    /**
     * Clears all cached transfer information.
     */
    fun clear() {
        transferAccountMap.clear()
        transferAmountMap.clear()
        transferTypeMap.clear()
        transferPurposeMap.clear()
        transactionToTransferMap.clear()
        transferClassificationsMap.clear()
    }
}
