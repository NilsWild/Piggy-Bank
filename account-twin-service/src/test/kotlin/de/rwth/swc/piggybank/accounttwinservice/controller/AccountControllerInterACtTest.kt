package de.rwth.swc.piggybank.accounttwinservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.rest.RestMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.rest.TestRestClient
import de.rwth.swc.piggybank.accounttwinservice.AmqpTestConfig
import de.rwth.swc.piggybank.accounttwinservice.InterACtConfig
import de.rwth.swc.piggybank.accounttwinservice.config.MockServerConfig
import de.rwth.swc.piggybank.accounttwinservice.config.RabbitMQTestConfig
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountRequest
import de.rwth.swc.piggybank.accounttwinservice.dto.AccountResponse
import de.rwth.swc.piggybank.accounttwinservice.dto.AmountDto
import de.rwth.swc.piggybank.accounttwinservice.util.RabbitMQTestUtils
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [MockServerConfig.Initializer::class, RabbitMQTestConfig.Initializer::class])
class AccountControllerInterACtTest {

    @LocalServerPort
    private lateinit var port: Number

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var testClient: TestRestClient

    @BeforeEach
    fun setUp() {
        testClient = InterACtConfig.testRestClient(webClientBuilder, port)
    }

    @InterACtTest
    @MethodSource("createAccountRequests")
    fun `should create account`(
        accountRequestStimulus: RestMessage.Request<AccountRequest>,
        transferGatewayResponse: RestMessage.Response<Boolean>
    ) {
        val accountRequest = accountRequestStimulus.body!!

        // Set up the MockServer to respond to the TransferGateway request
        try {
            MockServerConfig.setupAddMonitoredAccountExpectation(
                responseBody = transferGatewayResponse.body!!,
                statusCode = transferGatewayResponse.statusCode
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

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

        // Then verify that an ACCOUNT_CREATED event was sent to RabbitMQ
        val message = RabbitMQTestUtils.waitForEventType(rabbitTemplate, "ACCOUNT_CREATED", 2, TimeUnit.SECONDS)
        message shouldNotBe null
        // Convert the message body to a string and verify it contains the expected data
        val messageBody = String(message!!.body)
        messageBody shouldContain "\"eventType\":\"ACCOUNT_CREATED\""
        messageBody shouldContain "\"accountId\":\"${responseBody.id}\""
        messageBody shouldContain "\"accountType\":\"${accountRequest.type}\""
        messageBody shouldContain "\"accountIdentifier\":\"${accountRequest.identifier}\""
        messageBody shouldContain "\"value\":\"${accountRequest.initialBalance.value}\""
        messageBody shouldContain "\"currencyCode\":\"${accountRequest.initialBalance.currencyCode}\""

    }

    fun createAccountRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                // Request stimulus
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
                ),
                // Mocked Transfer Gateway response
                RestMessage.Response(
                    "/api/accounts-to-watch",
                    mapOf("Content-Type" to MediaType.APPLICATION_JSON_VALUE),
                    mapOf(),
                    true,
                    HttpStatus.CREATED.value()
                )
            ),
            Arguments.of(
                // Request stimulus
                RestMessage.Request(
                    "/api/accounts",
                    mapOf("X-Request-ID" to "create-account-request"),
                    mapOf(),
                    AccountRequest(
                        type = "PayPal",
                        identifier = "user@example.com",
                        initialBalance = AmountDto(
                            value = BigDecimal("500.50"),
                            currencyCode = "USD"
                        )
                    )
                ),
                // Mocked Transfer Gateway response
                RestMessage.Response(
                    "/api/accounts-to-watch",
                    mapOf("Content-Type" to MediaType.APPLICATION_JSON_VALUE),
                    mapOf(),
                    true,
                    HttpStatus.CREATED.value()
                )
            )
        )
    }
}
