package de.rwth.swc.piggybank.transfergateway.service

import de.rwth.swc.piggybank.transfergateway.client.AccountTwinServiceClient
import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.domain.Transfer
import de.rwth.swc.piggybank.transfergateway.dto.TransactionRequest
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency

class TransferServiceTest {

    private lateinit var transferService: TransferService
    private val accountService: AccountService = mockk()
    private val rabbitMQService: RabbitMQService = mockk()
    private val accountTwinServiceClient: AccountTwinServiceClient = mockk()

    @BeforeEach
    fun setUp() {
        // Create TransferService with mocked dependencies
        transferService = TransferService(
            accountService,
            rabbitMQService,
            accountTwinServiceClient
        )
    }

    @Test
    fun `should skip processing when neither source nor target account is monitored`() {
        // Given
        val sourceAccount = Account("BankAccount", "DE123456789")
        val targetAccount = Account("PayPal", "user@example.com")
        val amount = Amount(BigDecimal("100.00"), "EUR")
        val transfer = Transfer(
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            amount = amount,
            valuationTimestamp = Instant.now(),
            purpose = "Test transfer"
        )

        every { accountService.isMonitored(sourceAccount) } returns false
        every { accountService.isMonitored(targetAccount) } returns false

        // When
        val result = transferService.processTransfer(transfer)

        // Then
        result shouldBe true
        verify(exactly = 1) { accountService.isMonitored(sourceAccount) }
        verify(exactly = 1) { accountService.isMonitored(targetAccount) }
        verify(exactly = 0) { rabbitMQService.sendTransferEvent(any()) }
        verify(exactly = 0) { accountTwinServiceClient.sendTransaction(any()) }
    }

    @Test
    fun `should process transfer when source account is monitored`() {
        // Given
        val sourceAccount = Account("BankAccount", "DE123456789")
        val targetAccount = Account("PayPal", "user@example.com")
        val amount = Amount(BigDecimal("100.00"), "EUR")
        val transfer = Transfer(
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            amount = amount,
            valuationTimestamp = Instant.now(),
            purpose = "Test transfer"
        )

        every { accountService.isMonitored(sourceAccount) } returns true
        every { accountService.isMonitored(targetAccount) } returns false
        every { rabbitMQService.sendTransferEvent(any()) } just runs
        every { accountTwinServiceClient.sendTransaction(any()) } just runs

        // When
        val result = transferService.processTransfer(transfer)

        // Then
        result shouldBe true
        verify(exactly = 1) { accountService.isMonitored(sourceAccount) }
        verify(exactly = 1) { accountService.isMonitored(targetAccount) }
        verify(exactly = 1) { rabbitMQService.sendTransferEvent(transfer) }

        // Verify that the debit transaction was sent to AccountTwinService
        val transactionRequestSlot = slot<TransactionRequest>()
        verify(exactly = 1) { accountTwinServiceClient.sendTransaction(capture(transactionRequestSlot)) }

        // Verify the transaction request properties
        val capturedRequest = transactionRequestSlot.captured
        capturedRequest.account shouldBe sourceAccount
        capturedRequest.amount.value shouldBe amount.value
        capturedRequest.amount.currencyCode shouldBe amount.currencyCode
        capturedRequest.purpose shouldBe transfer.purpose
        capturedRequest.type shouldBe "DEBIT"
    }

    @Test
    fun `should process transfer when target account is monitored`() {
        // Given
        val sourceAccount = Account("BankAccount", "DE123456789")
        val targetAccount = Account("PayPal", "user@example.com")
        val amount = Amount(BigDecimal("100.00"), "EUR")
        val transfer = Transfer(
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            amount = amount,
            valuationTimestamp = Instant.now(),
            purpose = "Test transfer"
        )

        every { accountService.isMonitored(sourceAccount) } returns false
        every { accountService.isMonitored(targetAccount) } returns true
        every { rabbitMQService.sendTransferEvent(any()) } just runs
        every { accountTwinServiceClient.sendTransaction(any()) } just runs

        // When
        val result = transferService.processTransfer(transfer)

        // Then
        result shouldBe true
        verify(exactly = 1) { accountService.isMonitored(sourceAccount) }
        verify(exactly = 1) { accountService.isMonitored(targetAccount) }
        verify(exactly = 1) { rabbitMQService.sendTransferEvent(transfer) }

        // Verify that the credit transaction was sent to AccountTwinService
        val transactionRequestSlot = slot<TransactionRequest>()
        verify(exactly = 1) { accountTwinServiceClient.sendTransaction(capture(transactionRequestSlot)) }

        // Verify the transaction request properties
        val capturedRequest = transactionRequestSlot.captured
        capturedRequest.account shouldBe targetAccount
        capturedRequest.amount.value shouldBe amount.value
        capturedRequest.amount.currencyCode shouldBe amount.currencyCode
        capturedRequest.purpose shouldBe transfer.purpose
        capturedRequest.type shouldBe "CREDIT"
    }

    @Test
    fun `should process transfer when both source and target accounts are monitored`() {
        // Given
        val sourceAccount = Account("BankAccount", "DE123456789")
        val targetAccount = Account("PayPal", "user@example.com")
        val amount = Amount(BigDecimal("100.00"), "EUR")
        val transfer = Transfer(
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            amount = amount,
            valuationTimestamp = Instant.now(),
            purpose = "Test transfer"
        )

        every { accountService.isMonitored(sourceAccount) } returns true
        every { accountService.isMonitored(targetAccount) } returns true
        every { rabbitMQService.sendTransferEvent(any()) } just runs
        every { accountTwinServiceClient.sendTransaction(any()) } just runs

        // When
        val result = transferService.processTransfer(transfer)

        // Then
        result shouldBe true
        verify(exactly = 1) { accountService.isMonitored(sourceAccount) }
        verify(exactly = 1) { accountService.isMonitored(targetAccount) }
        verify(exactly = 1) { rabbitMQService.sendTransferEvent(transfer) }

        // Verify that both transactions were sent to AccountTwinService
        verify(exactly = 2) { accountTwinServiceClient.sendTransaction(any()) }

        // We can't use slots to capture both transactions since they overwrite each other,
        // but we can verify the order of operations
        verifyOrder {
            accountTwinServiceClient.sendTransaction(match<TransactionRequest> { it.type == "DEBIT" })
            accountTwinServiceClient.sendTransaction(match<TransactionRequest> { it.type == "CREDIT" })
        }
    }

    @Test
    fun `should return false when an exception occurs during processing`() {
        // Given
        val sourceAccount = Account("BankAccount", "DE123456789")
        val targetAccount = Account("PayPal", "user@example.com")
        val amount = Amount(BigDecimal("100.00"), "EUR")
        val transfer = Transfer(
            sourceAccount = sourceAccount,
            targetAccount = targetAccount,
            amount = amount,
            valuationTimestamp = Instant.now(),
            purpose = "Test transfer"
        )

        every { accountService.isMonitored(sourceAccount) } returns true
        every { accountService.isMonitored(targetAccount) } returns false
        every { rabbitMQService.sendTransferEvent(any()) } throws RuntimeException("Test exception")

        // When
        val result = transferService.processTransfer(transfer)

        // Then
        result shouldBe false
        verify(exactly = 1) { accountService.isMonitored(sourceAccount) }
        verify(exactly = 1) { accountService.isMonitored(targetAccount) }
        verify(exactly = 1) { rabbitMQService.sendTransferEvent(transfer) }
        verify(exactly = 0) { accountTwinServiceClient.sendTransaction(any()) }
    }
}
