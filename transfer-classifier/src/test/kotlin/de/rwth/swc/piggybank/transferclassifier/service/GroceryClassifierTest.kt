package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.domain.Transfer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GroceryClassifierTest {

    private val classifier = GroceryClassifier()

    @Test
    fun `should classify transfer with Aldi in purpose as grocery`() {
        // Given
        val transfer = createTransfer("Payment to Aldi for groceries")

        // When
        val result = classifier.classify(transfer)

        // Then
        result shouldBe true
    }

    @Test
    fun `should classify transfer with Lidl in purpose as grocery`() {
        // Given
        val transfer = createTransfer("Weekly shopping at Lidl")

        // When
        val result = classifier.classify(transfer)

        // Then
        result shouldBe true
    }

    @Test
    fun `should classify transfer with Edeka in purpose as grocery`() {
        // Given
        val transfer = createTransfer("Edeka purchase")

        // When
        val result = classifier.classify(transfer)

        // Then
        result shouldBe true
    }

    @Test
    fun `should not classify transfer without grocery store name in purpose as grocery`() {
        // Given
        val transfer = createTransfer("Payment to restaurant")

        // When
        val result = classifier.classify(transfer)

        // Then
        result shouldBe false
    }

    private fun createTransfer(purpose: String): Transfer {
        return Transfer(
            id = UUID.randomUUID(),
            sourceAccount = Account("IBAN", "DE123456789"),
            targetAccount = Account("IBAN", "DE987654321"),
            amount = Amount(BigDecimal("50.00"), "EUR"),
            valuationTimestamp = Instant.now(),
            purpose = purpose
        )
    }
}