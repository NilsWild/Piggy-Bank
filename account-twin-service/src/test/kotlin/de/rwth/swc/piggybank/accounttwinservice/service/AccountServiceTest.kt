package de.rwth.swc.piggybank.accounttwinservice.service

import de.rwth.swc.piggybank.accounttwinservice.client.TransferGatewayClient
import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.domain.TransactionType
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

class AccountServiceTest {

    private lateinit var accountService: AccountService
    private val accountRepository: AccountRepository = mockk()
    private val transactionRepository: TransactionRepository = mockk()
    private val rabbitMQService: RabbitMQService = mockk()
    private val transferGatewayClient: TransferGatewayClient = mockk()

    @BeforeEach
    fun setUp() {
        accountService = AccountService(
            accountRepository,
            transactionRepository,
            rabbitMQService,
            transferGatewayClient
        )
    }

    @Test
    fun `should create account successfully`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val balance = Amount(BigDecimal("100.00"), "EUR")
        val account = Account.create(type, identifier, balance)

        val transactionSlot = slot<Transaction>()

        every { accountRepository.existsByTypeAndIdentifier(type, identifier) } returns false
        every { accountRepository.save(account) } returns account
        every { transactionRepository.save(capture(transactionSlot)) } answers { transactionSlot.captured }
        every { rabbitMQService.sendAccountCreatedEvent(account) } just runs
        every { transferGatewayClient.addMonitoredAccount(account) } returns true

        // When
        val result = accountService.createAccount(account)

        // Then
        result shouldBe account

        verify { accountRepository.existsByTypeAndIdentifier(type, identifier) }
        verify { accountRepository.save(account) }
        verify { transactionRepository.save(any()) }
        verify { rabbitMQService.sendAccountCreatedEvent(account) }
        verify { transferGatewayClient.addMonitoredAccount(account) }

        // Verify the initial transaction
        val initialTransaction = transactionSlot.captured
        initialTransaction.accountId shouldBe account.id
        initialTransaction.account shouldBe account
        initialTransaction.amount shouldBe balance
        initialTransaction.purpose shouldBe "Initial balance"
        initialTransaction.type shouldBe TransactionType.DUMMY
    }

    @Test
    fun `should throw exception when creating account that already exists`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val balance = Amount(BigDecimal("100.00"), "EUR")
        val account = Account.create(type, identifier, balance)

        every { accountRepository.existsByTypeAndIdentifier(type, identifier) } returns true

        // When/Then
        val exception = assertThrows<IllegalStateException> {
            accountService.createAccount(account)
        }

        exception.message shouldBe "Account with type $type and identifier $identifier already exists"

        verify { accountRepository.existsByTypeAndIdentifier(type, identifier) }
        verify(exactly = 0) { accountRepository.save(any()) }
        verify(exactly = 0) { transactionRepository.save(any()) }
        verify(exactly = 0) { rabbitMQService.sendAccountCreatedEvent(any()) }
        verify(exactly = 0) { transferGatewayClient.addMonitoredAccount(any()) }
    }

    @Test
    fun `should get account by ID`() {
        // Given
        val accountId = "BankAccount:DE123456789"
        val account = createAccount(accountId)

        every { accountRepository.findById(accountId) } returns Optional.of(account)

        // When
        val result = accountService.getAccount(accountId)

        // Then
        result shouldBe account

        verify { accountRepository.findById(accountId) }
    }

    @Test
    fun `should return null when account does not exist`() {
        // Given
        val accountId = "BankAccount:DE123456789"

        every { accountRepository.findById(accountId) } returns Optional.empty()

        // When
        val result = accountService.getAccount(accountId)

        // Then
        result shouldBe null

        verify { accountRepository.findById(accountId) }
    }

    @Test
    fun `should get account by type and identifier`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val account = createAccount("$type:$identifier", type, identifier)

        every { accountRepository.findByTypeAndIdentifier(type, identifier) } returns account

        // When
        val result = accountService.getAccountByTypeAndIdentifier(type, identifier)

        // Then
        result shouldBe account

        verify { accountRepository.findByTypeAndIdentifier(type, identifier) }
    }

    @Test
    fun `should return null when account by type and identifier does not exist`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"

        every { accountRepository.findByTypeAndIdentifier(type, identifier) } returns null

        // When
        val result = accountService.getAccountByTypeAndIdentifier(type, identifier)

        // Then
        result shouldBe null

        verify { accountRepository.findByTypeAndIdentifier(type, identifier) }
    }

    @Test
    fun `should get all accounts`() {
        // Given
        val account1 = createAccount("BankAccount:DE123456789", "BankAccount", "DE123456789")
        val account2 = createAccount("PayPal:user@example.com", "PayPal", "user@example.com")
        val accounts = listOf(account1, account2)

        every { accountRepository.findAll() } returns accounts

        // When
        val result = accountService.getAllAccounts()

        // Then
        result shouldBe accounts
        result.size shouldBe 2
        result[0] shouldBe account1
        result[1] shouldBe account2

        verify { accountRepository.findAll() }
    }

    @Test
    fun `should get account transactions`() {
        // Given
        val accountId = "BankAccount:DE123456789"
        val pageable = Pageable.unpaged()
        val account = createAccount(accountId)
        val transaction1 = createTransaction(UUID.randomUUID(), account, "CREDIT", BigDecimal("100.00"))
        val transaction2 = createTransaction(UUID.randomUUID(), account, "DEBIT", BigDecimal("50.00"))
        val transactions = listOf(transaction1, transaction2)
        val page = PageImpl(transactions, pageable, transactions.size.toLong())

        every { transactionRepository.findByAccountId(accountId, pageable) } returns page

        // When
        val result = accountService.getAccountTransactions(accountId, pageable)

        // Then
        result shouldBe page
        result.content.size shouldBe 2
        result.content[0] shouldBe transaction1
        result.content[1] shouldBe transaction2

        verify { transactionRepository.findByAccountId(accountId, pageable) }
    }

    @Test
    fun `should delete account successfully`() {
        // Given
        val accountId = "BankAccount:DE123456789"
        val account = createAccount(accountId)
        val transaction1 = createTransaction(UUID.randomUUID(), account, "CREDIT", BigDecimal("100.00"))
        val transaction2 = createTransaction(UUID.randomUUID(), account, "DEBIT", BigDecimal("50.00"))
        val transactions = listOf(transaction1, transaction2)

        every { accountRepository.findById(accountId) } returns Optional.of(account)
        every { transactionRepository.findByAccountId(accountId) } returns transactions
        every { transactionRepository.deleteAll(transactions) } just runs
        every { accountRepository.delete(account) } just runs
        every { rabbitMQService.sendAccountDeletedEvent(account) } just runs

        // When
        val result = accountService.deleteAccount(accountId)

        // Then
        result shouldBe true

        verify { accountRepository.findById(accountId) }
        verify { transactionRepository.findByAccountId(accountId) }
        verify { transactionRepository.deleteAll(transactions) }
        verify { accountRepository.delete(account) }
        verify { rabbitMQService.sendAccountDeletedEvent(account) }
    }

    @Test
    fun `should return false when deleting non-existent account`() {
        // Given
        val accountId = "BankAccount:DE123456789"

        every { accountRepository.findById(accountId) } returns Optional.empty()

        // When
        val result = accountService.deleteAccount(accountId)

        // Then
        result shouldBe false

        verify { accountRepository.findById(accountId) }
        verify(exactly = 0) { transactionRepository.findByAccountId(any()) }
        verify(exactly = 0) { transactionRepository.deleteAll(any()) }
        verify(exactly = 0) { accountRepository.delete(any()) }
        verify(exactly = 0) { rabbitMQService.sendAccountDeletedEvent(any()) }
    }

    @Test
    fun `should update account balance with transaction`() {
        // Given
        val accountId = "BankAccount:DE123456789"
        val account = createAccount(accountId)
        val initialBalance = account.balance
        val transactionAmount = Amount(BigDecimal("50.00"), "EUR")
        val transaction = createTransaction(UUID.randomUUID(), account, "CREDIT", transactionAmount.value)

        val updatedAccount = account.copy(balance = initialBalance.add(transactionAmount))

        every { transactionRepository.save(transaction) } returns transaction
        every { accountRepository.save(any()) } returns updatedAccount
        every { rabbitMQService.sendAccountUpdatedEvent(updatedAccount, transaction) } just runs

        // When
        val result = accountService.updateAccountBalance(transaction)

        // Then
        result shouldBe updatedAccount
        result.balance.value shouldBe initialBalance.value.add(transactionAmount.value)

        verify { transactionRepository.save(transaction) }
        verify { accountRepository.save(any()) }
        verify { rabbitMQService.sendAccountUpdatedEvent(updatedAccount, transaction) }
    }

    private fun createAccount(
        id: String,
        type: String = "BankAccount",
        identifier: String = "DE123456789",
        balanceValue: BigDecimal = BigDecimal("100.00"),
        currencyCode: String = "EUR"
    ): Account {
        return Account(
            id = id,
            type = type,
            identifier = identifier,
            balance = Amount(balanceValue, currencyCode),
            createdAt = Instant.now()
        )
    }

    private fun createTransaction(
        id: UUID,
        account: Account,
        type: String = "CREDIT",
        value: BigDecimal = BigDecimal("100.00"),
        transferId: UUID = UUID.randomUUID()
    ): Transaction {
        return Transaction(
            id = id,
            transferId = transferId,
            accountId = account.id,
            account = account,
            amount = Amount(value, "EUR"),
            valuationTimestamp = Instant.now(),
            purpose = "Test transaction",
            type = TransactionType.valueOf(type),
            createdAt = Instant.now()
        )
    }
}
