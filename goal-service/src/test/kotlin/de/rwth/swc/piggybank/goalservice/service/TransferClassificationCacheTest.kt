package de.rwth.swc.piggybank.goalservice.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferClassificationCacheTest {

    private lateinit var cache: TransferClassificationCache

    private val transferId = UUID.randomUUID()
    private val transactionId = UUID.randomUUID()
    private val accountId = "test-account-id"
    private val amount = "100.00"
    private val type = "CREDIT"
    private val purpose = "Test purpose"

    @BeforeEach
    fun setup() {
        cache = TransferClassificationCache()
    }

    @Test
    fun `should store and retrieve transfer information using transfer ID`() {
        // Given
        cache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = amount,
            type = type,
            purpose = purpose
        )

        // When/Then
        cache.getAccountId(transferId) shouldBe accountId
        cache.getAmount(transferId) shouldBe amount
        cache.getType(transferId) shouldBe type
        cache.getPurpose(transferId) shouldBe purpose
    }

    @Test
    fun `should retrieve transfer information using transaction ID mapped to transfer ID`() {
        // Given
        // Store transfer information using the transfer ID
        cache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = amount,
            type = type,
            purpose = purpose
        )

        // Map the transaction ID to the transfer ID
        cache.mapTransactionToTransfer(transactionId, transferId)

        // When/Then
        // Retrieve the information using the transaction ID
        cache.getAccountId(transactionId) shouldBe accountId
        cache.getAmount(transactionId) shouldBe amount
        cache.getType(transactionId) shouldBe type
        cache.getPurpose(transactionId) shouldBe purpose
    }

    @Test
    fun `should return null when transfer ID is not found`() {
        // Given
        val nonExistentTransferId = UUID.randomUUID()

        // When/Then
        cache.getAccountId(nonExistentTransferId).shouldBeNull()
        cache.getAmount(nonExistentTransferId).shouldBeNull()
        cache.getType(nonExistentTransferId).shouldBeNull()
        cache.getPurpose(nonExistentTransferId).shouldBeNull()
    }

    @Test
    fun `should remove transfer information`() {
        // Given
        cache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = amount,
            type = type,
            purpose = purpose
        )

        // When
        cache.removeTransferInfo(transferId)

        // Then
        cache.getAccountId(transferId).shouldBeNull()
        cache.getAmount(transferId).shouldBeNull()
        cache.getType(transferId).shouldBeNull()
        cache.getPurpose(transferId).shouldBeNull()
    }

    @Test
    fun `should remove transaction-to-transfer mappings when removing transfer information`() {
        // Given
        cache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = amount,
            type = type,
            purpose = purpose
        )
        cache.mapTransactionToTransfer(transactionId, transferId)

        // When
        cache.removeTransferInfo(transferId)

        // Then
        cache.getAccountId(transactionId).shouldBeNull()
        cache.getAmount(transactionId).shouldBeNull()
        cache.getType(transactionId).shouldBeNull()
        cache.getPurpose(transactionId).shouldBeNull()
    }

    @Test
    fun `should handle multiple transaction IDs mapped to the same transfer ID`() {
        // Given
        cache.storeTransferInfo(
            transferId = transferId,
            accountId = accountId,
            amount = amount,
            type = type,
            purpose = purpose
        )
        val transactionId1 = UUID.randomUUID()
        val transactionId2 = UUID.randomUUID()
        cache.mapTransactionToTransfer(transactionId1, transferId)
        cache.mapTransactionToTransfer(transactionId2, transferId)

        // When/Then
        cache.getAccountId(transactionId1) shouldBe accountId
        cache.getAmount(transactionId1) shouldBe amount
        cache.getType(transactionId1) shouldBe type
        cache.getPurpose(transactionId1) shouldBe purpose

        cache.getAccountId(transactionId2) shouldBe accountId
        cache.getAmount(transactionId2) shouldBe amount
        cache.getType(transactionId2) shouldBe type
        cache.getPurpose(transactionId2) shouldBe purpose
    }
}