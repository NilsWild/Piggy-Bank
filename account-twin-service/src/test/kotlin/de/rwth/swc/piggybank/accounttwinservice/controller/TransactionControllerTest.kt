package de.rwth.swc.piggybank.accounttwinservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import de.rwth.swc.piggybank.accounttwinservice.domain.Transaction
import de.rwth.swc.piggybank.accounttwinservice.domain.TransactionType
import de.rwth.swc.piggybank.accounttwinservice.dto.AmountDto
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionResponse
import de.rwth.swc.piggybank.accounttwinservice.service.TransactionService
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class TransactionControllerTest {

    private lateinit var mockMvc: MockMvc
    private val transactionService: TransactionService = mockk()
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val transactionController = TransactionController(transactionService)
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
            .build()
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Test
    fun `should process transaction and return created status`() {
        // Given
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()
        val accountId = "account123"
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

        every { transactionService.transactionExists(transferId, accountId) } returns false
        every { transactionService.processTransaction(transactionRequest) } returns transaction

        // When/Then
        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(transactionId.toString()))
            .andExpect(jsonPath("$.transferId").value(transferId.toString()))
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.amount.value").value(amount.value.toDouble()))
            .andExpect(jsonPath("$.amount.currencyCode").value(amount.currencyCode))
            .andExpect(jsonPath("$.purpose").value(purpose))
            .andExpect(jsonPath("$.type").value(type))

        verify { transactionService.transactionExists(transferId, accountId) }
        verify { transactionService.processTransaction(transactionRequest) }
    }

    @Test
    fun `should return existing transaction when it already exists`() {
        // Given
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()
        val accountId = "account123"
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

        every { transactionService.transactionExists(transferId, accountId) } returns true
        every { transactionService.getTransactionByTransferIdAndAccountId(transferId, accountId) } returns transaction

        // When/Then
        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transactionId.toString()))
            .andExpect(jsonPath("$.transferId").value(transferId.toString()))
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.amount.value").value(amount.value.toDouble()))
            .andExpect(jsonPath("$.amount.currencyCode").value(amount.currencyCode))
            .andExpect(jsonPath("$.purpose").value(purpose))
            .andExpect(jsonPath("$.type").value(type))

        verify { transactionService.transactionExists(transferId, accountId) }
        verify { transactionService.getTransactionByTransferIdAndAccountId(transferId, accountId) }
        verify(exactly = 0) { transactionService.processTransaction(any()) }
    }

    @Test
    fun `should handle bad request when processing transaction`() {
        // Given
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()
        val accountId = "account123"
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

        every { transactionService.transactionExists(transferId, accountId) } returns false
        every { transactionService.processTransaction(transactionRequest) } throws IllegalStateException("Account not found")

        // When/Then
        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest))
        )
            .andExpect(status().isBadRequest)

        verify { transactionService.transactionExists(transferId, accountId) }
        verify { transactionService.processTransaction(transactionRequest) }
    }

    @Test
    fun `should get transaction by ID`() {
        // Given
        val transactionId = UUID.randomUUID()
        val transferId = UUID.randomUUID()
        val accountId = "account123"
        val amount = Amount(BigDecimal("100.00"), "EUR")
        val valuationTimestamp = Instant.now()
        val purpose = "Test transaction"
        val type = TransactionType.CREDIT
        val createdAt = Instant.now()

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
            amount = amount,
            valuationTimestamp = valuationTimestamp,
            purpose = purpose,
            type = type,
            createdAt = createdAt
        )

        every { transactionService.getTransaction(transactionId) } returns transaction

        // When/Then
        mockMvc.perform(
            get("/api/transactions/{transactionId}", transactionId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transactionId.toString()))
            .andExpect(jsonPath("$.transferId").value(transferId.toString()))
            .andExpect(jsonPath("$.accountId").value(accountId))
            .andExpect(jsonPath("$.amount.value").value(amount.value.toDouble()))
            .andExpect(jsonPath("$.amount.currencyCode").value(amount.currencyCode))
            .andExpect(jsonPath("$.purpose").value(purpose))
            .andExpect(jsonPath("$.type").value(type.name))
            .andExpect(jsonPath("$.createdAt").exists())

        verify { transactionService.getTransaction(transactionId) }
    }

    @Test
    fun `should return not found when transaction does not exist`() {
        // Given
        val transactionId = UUID.randomUUID()

        every { transactionService.getTransaction(transactionId) } returns null

        // When/Then
        mockMvc.perform(
            get("/api/transactions/{transactionId}", transactionId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)

        verify { transactionService.getTransaction(transactionId) }
    }

    @Test
    fun `should get transactions by account ID`() {
        // Given
        val accountId = "account123"

        val transaction1 = createTransaction(UUID.randomUUID(), accountId, "CREDIT", BigDecimal("100.00"))
        val transaction2 = createTransaction(UUID.randomUUID(), accountId, "DEBIT", BigDecimal("50.00"))

        val transactions = listOf(transaction1, transaction2)
        val pageable = PageRequest.of(0, 20, Sort.by("createdAt"))
        val page = PageImpl(transactions, pageable, transactions.size.toLong())

        // Use eq matcher for both parameters to ensure correct matching
        every { transactionService.getTransactionsByAccount(eq(accountId), eq(pageable)) } returns page

        // When/Then
        mockMvc.perform(
            get("/api/transactions/by-account/{accountId}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .param("page", "0")
                .param("size", "20")
                .param("sort", "createdAt")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].id").value(transaction1.id.toString()))
            .andExpect(jsonPath("$.content[1].id").value(transaction2.id.toString()))

        verify { transactionService.getTransactionsByAccount(eq(accountId), eq(pageable)) }
    }

    private fun createTransaction(
        id: UUID,
        accountId: String,
        type: String,
        value: BigDecimal
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
