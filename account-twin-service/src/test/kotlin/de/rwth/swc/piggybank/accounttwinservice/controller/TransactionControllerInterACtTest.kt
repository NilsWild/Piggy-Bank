package de.rwth.swc.piggybank.accounttwinservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.domain.rest.RestMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.rest.TestRestClient
import de.rwth.swc.piggybank.accounttwinservice.AmqpBaseTest
import de.rwth.swc.piggybank.accounttwinservice.InterACtConfig
import de.rwth.swc.piggybank.accounttwinservice.domain.Account
import de.rwth.swc.piggybank.accounttwinservice.domain.Amount
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountResponse
import de.rwth.swc.piggybank.accounttwinservice.dto.AmountDto
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.TransactionResponse
import de.rwth.swc.piggybank.accounttwinservice.repository.AccountRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionControllerInterACtTest : AmqpBaseTest() {

    @LocalServerPort
    private lateinit var port: Number

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    @Autowired
    private lateinit var accountRepository: AccountRepository

    private lateinit var testClient: TestRestClient

    @BeforeEach
    fun setUp() {
        testClient = InterACtConfig.testRestClient(webClientBuilder, port)
    }

    /**
     * Helper method to create an account directly in the database with a specific ID
     */
    private fun createAccount(accountId: String, type: String, identifier: String, initialBalance: BigDecimal, currencyCode: String) {
        val account = Account(
            id = accountId,
            type = type,
            identifier = identifier,
            balance = Amount(initialBalance, currencyCode)
        )

        accountRepository.save(account)
    }

    @InterACtTest
    @MethodSource("processTransactionRequests")
    fun `should process transaction with different parameters`(transactionRequestStimulus: RestMessage.Request<TransactionRequest>) {
        val transactionRequest = transactionRequestStimulus.body!!

        // Create test accounts with the ID and currency specified in the request
        // This ensures the account exists with the exact ID and currency that the request expects
        createAccount(
            accountId = transactionRequest.accountId,
            type = "BankAccount",
            identifier = "DE123456789",
            initialBalance = BigDecimal("1000.00"),
            currencyCode = transactionRequest.amount.currencyCode
        )

        // Send the request and get the response using TestRestClient
        val response = testClient.prepare(HttpMethod.POST, transactionRequestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        response shouldNotBe null

        // Verify the response status is CREATED (201)
        response!!.statusCode shouldBe HttpStatus.CREATED

        // Parse the response body to TransactionResponse
        val responseBody = objectMapper.readValue(response.body, TransactionResponse::class.java)

        // Verify the response body contains the expected data
        responseBody.id shouldBe transactionRequest.id
        responseBody.transferId shouldBe transactionRequest.transferId
        responseBody.accountId shouldBe transactionRequest.accountId
        responseBody.amount.value shouldBe transactionRequest.amount.value
        responseBody.amount.currencyCode shouldBe transactionRequest.amount.currencyCode
        responseBody.valuationTimestamp shouldBe transactionRequest.valuationTimestamp
        responseBody.purpose shouldBe transactionRequest.purpose
        responseBody.type shouldBe transactionRequest.type
        responseBody.sourceAccount shouldBe transactionRequest.sourceAccount
        responseBody.destinationAccount shouldBe transactionRequest.destinationAccount
        responseBody.createdAt shouldNotBe null

        // Wait for the ACCOUNT_UPDATED event to be sent to RabbitMQ
        val message = waitForEventType("ACCOUNT_UPDATED", 10, TimeUnit.SECONDS)

        // Verify the message
        message shouldNotBe null
        val messageBody = String(message!!.body)

        // Verify the message contains the expected data
        messageBody shouldContain "\"eventType\":\"ACCOUNT_UPDATED\""
        messageBody shouldContain "\"transactionId\":\"${transactionRequest.id}\""
        messageBody shouldContain "\"accountId\":\"${transactionRequest.accountId}\""
        messageBody shouldContain "\"transactionAmount\":{\"value\":${transactionRequest.amount.value},\"currencyCode\":\"${transactionRequest.amount.currencyCode}\"}"
        messageBody shouldContain "\"transactionType\":\"${transactionRequest.type}\""
        messageBody shouldContain "\"transactionPurpose\":\"${transactionRequest.purpose}\""
    }

    fun processTransactionRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request(
                    "/api/transactions",
                    mapOf(),
                    mapOf(),
                    TransactionRequest(
                        id = UUID.randomUUID(),
                        transferId = UUID.randomUUID(),
                        accountId = "account-123",
                        amount = AmountDto(
                            value = BigDecimal("100.00"),
                            currencyCode = "EUR"
                        ),
                        valuationTimestamp = Instant.now(),
                        purpose = "Test transaction",
                        type = "CREDIT",
                        sourceAccount = "source-account-123",
                        destinationAccount = "destination-account-123"
                    )
                )
            ),
            Arguments.of(
                RestMessage.Request(
                    "/api/transactions",
                    mapOf(),
                    mapOf(),
                    TransactionRequest(
                        id = UUID.randomUUID(),
                        transferId = UUID.randomUUID(),
                        accountId = "account-456",
                        amount = AmountDto(
                            value = BigDecimal("200.00"),
                            currencyCode = "USD"
                        ),
                        valuationTimestamp = Instant.now(),
                        purpose = "Another test transaction",
                        type = "DEBIT",
                        sourceAccount = "source-account-456",
                        destinationAccount = "destination-account-456"
                    )
                )
            )
        )
    }
}
