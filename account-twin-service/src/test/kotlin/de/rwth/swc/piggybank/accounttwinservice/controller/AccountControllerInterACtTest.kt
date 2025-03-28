package de.rwth.swc.piggybank.accounttwinservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.domain.rest.RestMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.rest.TestRestClient
import de.rwth.swc.piggybank.accounttwinservice.AmqpBaseTest
import de.rwth.swc.piggybank.accounttwinservice.InterACtConfig
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountResponse
import de.rwth.swc.piggybank.accounttwinservice.dto.AmountDto
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
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountControllerInterACtTest : AmqpBaseTest() {

    @LocalServerPort
    private lateinit var port: Number

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    private lateinit var testClient: TestRestClient

    @BeforeEach
    fun setUp() {
        testClient = InterACtConfig.testRestClient(webClientBuilder, port)
    }

    @InterACtTest
    @MethodSource("createAccountRequests")
    fun `should create account with different parameters`(accountRequestStimulus: RestMessage.Request<AccountRequest>) {
        val accountRequest = accountRequestStimulus.body!!
        // Send the request and get the response using TestRestClient
        val response = testClient.prepare(HttpMethod.POST, accountRequestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        response shouldNotBe null

        // Verify the response status is CREATED (201)
        response!!.statusCode shouldBe HttpStatus.CREATED

        // Parse the response body to AccountResponse
        val responseBody = objectMapper.readValue(response.body, AccountResponse::class.java)

        // Verify the response body contains the expected data
        responseBody.id shouldNotBe null
        responseBody.type shouldBe accountRequest.type
        responseBody.identifier shouldBe accountRequest.identifier
        responseBody.balance.value shouldBe accountRequest.initialBalance.value
        responseBody.balance.currencyCode shouldBe accountRequest.initialBalance.currencyCode
        responseBody.createdAt shouldNotBe null
        responseBody.transactions shouldBe null

        // Then
        // Wait for the ACCOUNT_CREATED event to be sent to RabbitMQ
        val message = waitForEventType("ACCOUNT_CREATED", 10, TimeUnit.SECONDS)

        // Verify the message
        message shouldNotBe null
        val messageBody = String(message!!.body)

        // Verify the message contains the expected data
        messageBody shouldContain "\"eventType\":\"ACCOUNT_CREATED\""
        messageBody shouldContain "\"accountType\":\"${accountRequest.type}\""
        messageBody shouldContain "\"accountIdentifier\":\"${accountRequest.identifier}\""
        messageBody shouldContain "\"value\":${accountRequest.initialBalance.value}"
        messageBody shouldContain "\"currencyCode\":\"${accountRequest.initialBalance.currencyCode}\""
    }

    fun createAccountRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request(
                    "/api/accounts",
                    mapOf(),
                    mapOf(),
                    AccountRequest(
                        type = "BankAccount",
                        identifier = "DE123456789",
                        initialBalance = AmountDto(
                            value = BigDecimal("1000.00"),
                            currencyCode = "EUR"
                        )
                    )
                )
            )
        )
    }
}
