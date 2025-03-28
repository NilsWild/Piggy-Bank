package de.rwth.swc.piggybank.accounttwinservice.service

import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.domain.TransactionType
import de.rwth.swc.piggybank.accounttwinservice.dto.AmountDto
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionRequest
import de.rwth.swc.piggybank.accounttwinservice.repository.AccountRepository
import de.rwth.swc.piggybank.accounttwinservice.repository.TransactionRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class TransactionServiceTest {

    private lateinit var transactionService: TransactionService
    private val accountRepository: AccountRepository = mockk()
    private val transactionRepository: TransactionRepository = mockk()
    private val accountService: AccountService = mockk()

    @BeforeEach
    fun setUp() {
        transactionService = TransactionService(
            accountRepository,
            transactionRepository,
            accountService
        )
    }

    @Test
    fun `should process transaction successfully`() {
        // Given
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()
        val accountId = "BankAccount:DE123456789"
        val amount = AmountDto(BigDecimal("100.00"), "EUR")
        val valuationTimestamp = Instant.now()
        val purpose = "Test transaction"
        val type = "CREDIT"

        val transactionRequest = TransactionRequest(
            id = transactionId,
            transferId = transferId,
            accountId = accountId,
            amount = amount,
            valuationTimestamp = valuationTimestamp,
            purpose = purpose,
            type = type
        )

        val account = Account(
            id = accountId,
            type = "BankAccount",
            identifier = "DE123456789",
            balance = Amount(BigDecimal.ZERO, "EUR"),
            createdAt = Instant.now()
        )

        val transaction = Transaction(
            id = transactionId,
            transferId = transferId,
            accountId = accountId,
            account = account,
            amount = amount.toDomain(),
            valuationTimestamp = valuationTimestamp,
            purpose = purpose,
            type = TransactionType.valueOf(type),
            createdAt = Instant.now()
        )

        every { accountRepository.findById(accountId) } returns Optional.of(account)
        every { accountService.updateAccountBalance(any()) } returns account

        // When
        val result = transactionService.processTransaction(transactionRequest)

        // Then
        result.id shouldBe transactionId
        result.transferId shouldBe transferId
        result.accountId shouldBe accountId
        result.amount.value shouldBe amount.value
        result.amount.currencyCode shouldBe amount.currencyCode
        result.purpose shouldBe purpose
        result.type shouldBe TransactionType.valueOf(type)

        verify { accountRepository.findById(accountId) }
        verify { accountService.updateAccountBalance(any()) }
    }

    @Test
    fun `should throw exception when account does not exist`() {
        // Given
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()
        val accountId = "BankAccount:DE123456789"
        val amount = AmountDto(BigDecimal("100.00"), "EUR")
        val valuationTimestamp = Instant.now()
        val purpose = "Test transaction"
        val type = "CREDIT"

        val transactionRequest = TransactionRequest(
            id = transactionId,
            transferId = transferId,
            accountId = accountId,
            amount = amount,
            valuationTimestamp = valuationTimestamp,
            purpose = purpose,
            type = type
        )

        every { accountRepository.findById(accountId) } returns Optional.empty()

        // When/Then
        val exception = assertThrows<IllegalStateException> {
            transactionService.processTransaction(transactionRequest)
        }

        exception.message shouldBe "Account with ID $accountId not found"

        verify { accountRepository.findById(accountId) }
        verify(exactly = 0) { accountService.updateAccountBalance(any()) }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `should get transaction by ID`() {
        // Given
        val transactionId = UUID.randomUUID()
        val transaction = createTransaction(transactionId)

        every { transactionRepository.findById(transactionId) } returns Optional.of(transaction)

        // When
        val result = transactionService.getTransaction(transactionId)

        // Then
        result shouldBe transaction

        verify { transactionRepository.findById(transactionId) }
    }

    @Test
    fun `should return null when transaction does not exist`() {
        // Given
        val transactionId = UUID.randomUUID()

        every { transactionRepository.findById(transactionId) } returns Optional.empty()

        // When
        val result = transactionService.getTransaction(transactionId)

        // Then
        result shouldBe null

        verify { transactionRepository.findById(transactionId) }
    }

    @Test
    fun `should get transactions by account`() {
        // Given
        val accountId = "BankAccount:DE123456789"
        val pageable = Pageable.unpaged()
        val transaction1 = createTransaction(UUID.randomUUID(), accountId, "CREDIT", BigDecimal("100.00"))
        val transaction2 = createTransaction(UUID.randomUUID(), accountId, "DEBIT", BigDecimal("50.00"))
        val transactions = listOf(transaction1, transaction2)
        val page = PageImpl(transactions, pageable, transactions.size.toLong())

        every { transactionRepository.findByAccountId(accountId, pageable) } returns page

        // When
        val result = transactionService.getTransactionsByAccount(accountId, pageable)

        // Then
        result shouldBe page
        result.content.size shouldBe 2
        result.content[0] shouldBe transaction1
        result.content[1] shouldBe transaction2

        verify { transactionRepository.findByAccountId(accountId, pageable) }
    }

    @Test
    fun `should get transaction by transfer ID and account ID`() {
        // Given
        val transferId = UUID.randomUUID()
        val accountId = "BankAccount:DE123456789"
        val transaction = createTransaction(UUID.randomUUID(), accountId, "CREDIT", BigDecimal("100.00"), transferId)

        every { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) } returns transaction

        // When
        val result = transactionService.getTransactionByTransferIdAndAccountId(transferId, accountId)

        // Then
        result shouldBe transaction

        verify { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) }
    }

    @Test
    fun `should return null when transaction by transfer ID and account ID does not exist`() {
        // Given
        val transferId = UUID.randomUUID()
        val accountId = "BankAccount:DE123456789"

        every { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) } returns null

        // When
        val result = transactionService.getTransactionByTransferIdAndAccountId(transferId, accountId)

        // Then
        result shouldBe null

        verify { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) }
    }

    @Test
    fun `should check if transaction exists`() {
        // Given
        val transferId = UUID.randomUUID()
        val accountId = "BankAccount:DE123456789"
        val transaction = createTransaction(UUID.randomUUID(), accountId, "CREDIT", BigDecimal("100.00"), transferId)

        every { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) } returns transaction

        // When
        val result = transactionService.transactionExists(transferId, accountId)

        // Then
        result shouldBe true

        verify { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) }
    }

    @Test
    fun `should return false when transaction does not exist`() {
        // Given
        val transferId = UUID.randomUUID()
        val accountId = "BankAccount:DE123456789"

        every { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) } returns null

        // When
        val result = transactionService.transactionExists(transferId, accountId)

        // Then
        result shouldBe false

        verify { transactionRepository.findByTransferIdAndAccountId(transferId, accountId) }
    }

    private fun createTransaction(
        id: UUID,
        accountId: String = "BankAccount:DE123456789",
        type: String = "CREDIT",
        value: BigDecimal = BigDecimal("100.00"),
        transferId: UUID = UUID.randomUUID()
    ): Transaction {
        val account = Account(
            id = accountId,
            type = "BankAccount",
            identifier = "DE123456789",
            balance = Amount(BigDecimal.ZERO, "EUR"),
            createdAt = Instant.now()
        )

        return Transaction(
            id = id,
            transferId = transferId,
            accountId = accountId,
            account = account,
            amount = Amount(value, "EUR"),
            valuationTimestamp = Instant.now(),
            purpose = "Test transaction",
            type = TransactionType.valueOf(type),
            createdAt = Instant.now()
        )
    }
}
