package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transferclassifier.domain.Account
import de.rwth.swc.piggybank.transferclassifier.domain.Amount
import de.rwth.swc.piggybank.transferclassifier.domain.Transfer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HolidayClassifierTest {

    private val classifier = HolidayClassifier()

    @Test
    fun `should classify transfer with hotel in purpose as holiday`() {
        // Given
        val transfer = createTransfer("Payment to Hilton Hotel")

        // When
        val result = classifier.classify(transfer)

        // Then
        result shouldBe true
    }

    @Test
    fun `should classify transfer with rental in purpose as holiday`() {
        // Given
        val transfer = createTransfer("Car rental for vacation")

        // When
        val result = classifier.classify(transfer)

        // Then
        result shouldBe true
    }

    @Test
    fun `should not classify transfer without holiday keywords in purpose as holiday`() {
        // Given
        val transfer = createTransfer("Payment to restaurant")

        // When
        val result = classifier.classify(transfer)

        // Then
        result shouldBe false
    }

    @Test
    fun `should classify transfer with new keywords in purpose as holiday`() {
        // Given
        val transferWithResort = createTransfer("Payment to Mountain Resort")
        val transferWithBeach = createTransfer("Beach access fee")
        val transferWithTravel = createTransfer("Travel agency booking")
        val transferWithHoliday = createTransfer("Holiday package payment")

        // When
        val resortResult = classifier.classify(transferWithResort)
        val beachResult = classifier.classify(transferWithBeach)
        val travelResult = classifier.classify(transferWithTravel)
        val holidayResult = classifier.classify(transferWithHoliday)

        // Then
        resortResult shouldBe true
        beachResult shouldBe true
        travelResult shouldBe true
        holidayResult shouldBe true
    }

    private fun createTransfer(purpose: String): Transfer {
        return Transfer(
            id = UUID.randomUUID(),
            sourceAccount = Account("IBAN", "DE123456789"),
            targetAccount = Account("IBAN", "DE987654321"),
            amount = Amount(BigDecimal("150.00"), "EUR"),
            valuationTimestamp = Instant.now(),
            purpose = purpose
        )
    }
}
