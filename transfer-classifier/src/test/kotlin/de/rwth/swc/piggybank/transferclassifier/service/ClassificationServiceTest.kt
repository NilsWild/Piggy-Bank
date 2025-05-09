package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.domain.Transfer
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassificationServiceTest {

    private val groceryClassifier = mockk<TransferClassifier>()
    private val holidayClassifier = mockk<TransferClassifier>()
    private val classificationService = ClassificationService(listOf(groceryClassifier, holidayClassifier))

    @Test
    fun `should classify transfer with both grocery and holiday keywords`() {
        // Given
        val transferId = UUID.randomUUID()
        val transfer = createTransfer(transferId, "Payment to Aldi for hotel supplies")
        
        every { groceryClassifier.name } returns "Grocery"
        every { holidayClassifier.name } returns "Holiday"
        every { groceryClassifier.classify(transfer) } returns true
        every { holidayClassifier.classify(transfer) } returns true

        // When
        val result = classificationService.classifyTransfer(transfer)

        // Then
        result.transferId shouldBe transferId
        result.classifications shouldContainExactlyInAnyOrder listOf("Grocery", "Holiday")
    }

    @Test
    fun `should classify transfer with only grocery keywords`() {
        // Given
        val transferId = UUID.randomUUID()
        val transfer = createTransfer(transferId, "Payment to Lidl")
        
        every { groceryClassifier.name } returns "Grocery"
        every { holidayClassifier.name } returns "Holiday"
        every { groceryClassifier.classify(transfer) } returns true
        every { holidayClassifier.classify(transfer) } returns false

        // When
        val result = classificationService.classifyTransfer(transfer)

        // Then
        result.transferId shouldBe transferId
        result.classifications shouldContainExactlyInAnyOrder listOf("Grocery")
    }

    @Test
    fun `should classify transfer with only holiday keywords`() {
        // Given
        val transferId = UUID.randomUUID()
        val transfer = createTransfer(transferId, "Payment for hotel")
        
        every { groceryClassifier.name } returns "Grocery"
        every { holidayClassifier.name } returns "Holiday"
        every { groceryClassifier.classify(transfer) } returns false
        every { holidayClassifier.classify(transfer) } returns true

        // When
        val result = classificationService.classifyTransfer(transfer)

        // Then
        result.transferId shouldBe transferId
        result.classifications shouldContainExactlyInAnyOrder listOf("Holiday")
    }

    @Test
    fun `should return empty classifications for transfer without keywords`() {
        // Given
        val transferId = UUID.randomUUID()
        val transfer = createTransfer(transferId, "Payment to restaurant")
        
        every { groceryClassifier.name } returns "Grocery"
        every { holidayClassifier.name } returns "Holiday"
        every { groceryClassifier.classify(transfer) } returns false
        every { holidayClassifier.classify(transfer) } returns false

        // When
        val result = classificationService.classifyTransfer(transfer)

        // Then
        result.transferId shouldBe transferId
        result.classifications shouldBe emptyList()
    }

    private fun createTransfer(id: UUID, purpose: String): Transfer {
        return Transfer(
            id = id,
            sourceAccount = Account("IBAN", "DE123456789"),
            targetAccount = Account("IBAN", "DE987654321"),
            amount = Amount(BigDecimal("100.00"), "EUR"),
            valuationTimestamp = Instant.now(),
            purpose = purpose
        )
    }
}