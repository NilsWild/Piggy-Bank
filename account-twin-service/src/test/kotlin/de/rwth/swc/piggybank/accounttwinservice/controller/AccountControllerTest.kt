package de.rwth.swc.piggybank.accounttwinservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.domain.TransactionType
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.AmountDto
import de.rwth.swc.piggybank.accounttwinservice.service.AccountService
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AccountControllerTest {

    private lateinit var mockMvc: MockMvc
    private val accountService: AccountService = mockk()
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val accountController = AccountController(accountService)
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Test
    fun `should create account and return created status`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val initialBalance = AmountDto(BigDecimal("100.00"), "EUR")
        val accountRequest = AccountRequest(type, identifier, initialBalance)

        val account = Account.create(type, identifier, initialBalance.toDomain())
        
        every { accountService.createAccount(any()) } returns account

        // When/Then
        mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("$type:$identifier"))
            .andExpect(jsonPath("$.type").value(type))
            .andExpect(jsonPath("$.identifier").value(identifier))
            .andExpect(jsonPath("$.balance.value").value(initialBalance.value.toDouble()))
            .andExpect(jsonPath("$.balance.currencyCode").value(initialBalance.currencyCode))
            .andExpect(jsonPath("$.createdAt").exists())

        verify { accountService.createAccount(any()) }
    }

    @Test
    fun `should handle conflict when creating account that already exists`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val initialBalance = AmountDto(BigDecimal("100.00"), "EUR")
        val accountRequest = AccountRequest(type, identifier, initialBalance)

        every { accountService.createAccount(any()) } throws IllegalStateException("Account already exists")

        // When/Then
        mockMvc.perform(
            post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountRequest))
        )
            .andExpect(status().isConflict)

        verify { accountService.createAccount(any()) }
    }

    @Test
    fun `should get account by ID`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val accountId = "$type:$identifier"
        val balance = Amount(BigDecimal("100.00"), "EUR")
        val createdAt = Instant.now()

        val account = Account(
            id = accountId,
            type = type,
            identifier = identifier,
            balance = balance,
            createdAt = createdAt
        )

        every { accountService.getAccount(accountId) } returns account

        // When/Then
        mockMvc.perform(
            get("/api/accounts/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(accountId))
            .andExpect(jsonPath("$.type").value(type))
            .andExpect(jsonPath("$.identifier").value(identifier))
            .andExpect(jsonPath("$.balance.value").value(balance.value.toDouble()))
            .andExpect(jsonPath("$.balance.currencyCode").value(balance.currencyCode))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.transactions").doesNotExist())

        verify { accountService.getAccount(accountId) }
    }

    @Test
    fun `should get account by ID with transactions`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val accountId = "$type:$identifier"
        val balance = Amount(BigDecimal("100.00"), "EUR")
        val createdAt = Instant.now()

        val account = Account(
            id = accountId,
            type = type,
            identifier = identifier,
            balance = balance,
            createdAt = createdAt
        )

        val transaction1 = createTransaction(UUID.randomUUID(), accountId, account, "CREDIT", BigDecimal("100.00"))
        val transaction2 = createTransaction(UUID.randomUUID(), accountId, account, "DEBIT", BigDecimal("50.00"))

        val transactions = listOf(transaction1, transaction2)
        val page = PageImpl(transactions, Pageable.unpaged(), transactions.size.toLong())

        every { accountService.getAccount(accountId) } returns account
        every { accountService.getAccountTransactions(accountId, any()) } returns page

        // When/Then
        mockMvc.perform(
            get("/api/accounts/{accountId}", accountId)
                .param("includeTransactions", "true")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(accountId))
            .andExpect(jsonPath("$.type").value(type))
            .andExpect(jsonPath("$.identifier").value(identifier))
            .andExpect(jsonPath("$.balance.value").value(balance.value.toDouble()))
            .andExpect(jsonPath("$.balance.currencyCode").value(balance.currencyCode))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.transactions.length()").value(2))
            .andExpect(jsonPath("$.transactions[0].id").value(transaction1.id.toString()))
            .andExpect(jsonPath("$.transactions[1].id").value(transaction2.id.toString()))

        verify { accountService.getAccount(accountId) }
        verify { accountService.getAccountTransactions(accountId, any()) }
    }

    @Test
    fun `should return not found when account does not exist`() {
        // Given
        val accountId = "BankAccount:DE123456789"

        every { accountService.getAccount(accountId) } returns null

        // When/Then
        mockMvc.perform(
            get("/api/accounts/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)

        verify { accountService.getAccount(accountId) }
    }

    @Test
    fun `should get account by type and identifier`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val accountId = "$type:$identifier"
        val balance = Amount(BigDecimal("100.00"), "EUR")
        val createdAt = Instant.now()

        val account = Account(
            id = accountId,
            type = type,
            identifier = identifier,
            balance = balance,
            createdAt = createdAt
        )

        every { accountService.getAccountByTypeAndIdentifier(type, identifier) } returns account

        // When/Then
        mockMvc.perform(
            get("/api/accounts/by-type-and-identifier")
                .param("type", type)
                .param("identifier", identifier)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(accountId))
            .andExpect(jsonPath("$.type").value(type))
            .andExpect(jsonPath("$.identifier").value(identifier))
            .andExpect(jsonPath("$.balance.value").value(balance.value.toDouble()))
            .andExpect(jsonPath("$.balance.currencyCode").value(balance.currencyCode))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.transactions").doesNotExist())

        verify { accountService.getAccountByTypeAndIdentifier(type, identifier) }
    }

    @Test
    fun `should get all accounts`() {
        // Given
        val account1 = createAccount("BankAccount", "DE123456789", BigDecimal("100.00"), "EUR")
        val account2 = createAccount("PayPal", "user@example.com", BigDecimal("50.00"), "USD")

        val accounts = listOf(account1, account2)

        every { accountService.getAllAccounts() } returns accounts

        // When/Then
        mockMvc.perform(
            get("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(account1.id))
            .andExpect(jsonPath("$[1].id").value(account2.id))

        verify { accountService.getAllAccounts() }
    }

    @Test
    fun `should get account balance`() {
        // Given
        val type = "BankAccount"
        val identifier = "DE123456789"
        val accountId = "$type:$identifier"
        val balance = Amount(BigDecimal("100.00"), "EUR")
        val createdAt = Instant.now()

        val account = Account(
            id = accountId,
            type = type,
            identifier = identifier,
            balance = balance,
            createdAt = createdAt
        )

        every { accountService.getAccount(accountId) } returns account

        // When/Then
        mockMvc.perform(
            get("/api/accounts/{accountId}/balance", accountId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(accountId))
            .andExpect(jsonPath("$.balance.value").value(balance.value.toDouble()))
            .andExpect(jsonPath("$.balance.currencyCode").value(balance.currencyCode))

        verify { accountService.getAccount(accountId) }
    }

    @Test
    fun `should delete account`() {
        // Given
        val accountId = "BankAccount:DE123456789"

        every { accountService.deleteAccount(accountId) } returns true

        // When/Then
        mockMvc.perform(
            delete("/api/accounts/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNoContent)

        verify { accountService.deleteAccount(accountId) }
    }

    @Test
    fun `should return not found when deleting non-existent account`() {
        // Given
        val accountId = "BankAccount:DE123456789"

        every { accountService.deleteAccount(accountId) } returns false

        // When/Then
        mockMvc.perform(
            delete("/api/accounts/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)

        verify { accountService.deleteAccount(accountId) }
    }

    private fun createAccount(
        type: String,
        identifier: String,
        balanceValue: BigDecimal,
        currencyCode: String
    ): Account {
        val accountId = "$type:$identifier"
        val balance = Amount(balanceValue, currencyCode)
        return Account(
            id = accountId,
            type = type,
            identifier = identifier,
            balance = balance,
            createdAt = Instant.now()
        )
    }

    private fun createTransaction(
        id: UUID,
        accountId: String,
        account: Account,
        type: String,
        value: BigDecimal
    ): Transaction {
        return Transaction(
            id = id,
            transferId = UUID.randomUUID(),
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