package de.rwth.swc.piggybank.transfergateway.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.rest.RestMessage
import de.interact.domain.testobservation.config.Configuration
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.rest.TestRestClient
import de.rwth.swc.piggybank.transfergateway.AmqpTestConfig
import de.rwth.swc.piggybank.transfergateway.InterACtConfig
import de.rwth.swc.piggybank.transfergateway.config.MockServerConfig
import de.rwth.swc.piggybank.transfergateway.config.RabbitMQTestConfig
import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.dto.TransactionResponse
import de.rwth.swc.piggybank.transfergateway.dto.TransferRequest
import de.rwth.swc.piggybank.transfergateway.service.AccountService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.Instant
import java.util.stream.Stream

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [RabbitMQTestConfig.Initializer::class, MockServerConfig.Initializer::class])
class TransferControllerInterACtTest {

    @LocalServerPort
    private lateinit var port: Number

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    @Autowired
    private lateinit var accountService: AccountService

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var testClient: TestRestClient

    @BeforeEach
    fun setUp() {
        testClient = InterACtConfig.testRestClient(webClientBuilder, port)
        // Clear any existing accounts
        accountService.getAllMonitoredAccounts().forEach { accountService.removeMonitoredAccount(it) }
    }

    @InterACtTest
    @MethodSource("handleTransferRequests")
    fun `should handle transfer request when source account is monitored`(
        requestStimulus: RestMessage.Request<TransferRequest>,
        transactionResponse: RestMessage.Response<TransactionResponse>
    ) {
        val transferRequest = requestStimulus.body!!

        // Add the source account to the monitored accounts
        accountService.addMonitoredAccount(transferRequest.sourceAccount)

        // Set up the MockServer to respond to the AccountTwinService request
        MockServerConfig.setupSendTransactionExpectation(transactionResponse.statusCode)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.CREATED
    }

    @InterACtTest
    @MethodSource("handleTransferRequests")
    fun `should handle transfer request when target account is monitored`(
        requestStimulus: RestMessage.Request<TransferRequest>,
        transactionResponse: RestMessage.Response<TransactionResponse>
    ) {
        val transferRequest = requestStimulus.body!!

        // Add the target account to the monitored accounts
        accountService.addMonitoredAccount(transferRequest.targetAccount)

        // Set up the MockServer to respond to the AccountTwinService request
        MockServerConfig.setupSendTransactionExpectation(transactionResponse.statusCode)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.CREATED
    }

    @InterACtTest
    @MethodSource("handleTransferRequests")
    fun `should handle transfer request when neither account is monitored`(requestStimulus: RestMessage.Request<TransferRequest>) {
        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.CREATED
    }

    @InterACtTest
    @MethodSource("invalidTransferRequests")
    fun `should return bad request for invalid transfer request`(requestStimulus: RestMessage.Request<String>) {
        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    fun handleTransferRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request(
                    "/api/transfers",
                    mapOf(),
                    mapOf(),
                    TransferRequest(
                        sourceAccount = Account("BankAccount", "DE123456789"),
                        targetAccount = Account("PayPal", "user@example.com"),
                        amount = Amount(BigDecimal("100.00"), "EUR"),
                        valuationTimestamp = Instant.parse("2023-01-01T12:00:00Z"),
                        purpose = "Test transfer"
                    )
                ),
                RestMessage.Response(
                    "/api/transactions",
                    mapOf(),
                    mapOf(),
                    null,
                    201
                )
            ),
            Arguments.of(
                RestMessage.Request(
                    "/api/transfers",
                    mapOf("X-Request-ID" to "test-request-id"),
                    mapOf(),
                    TransferRequest(
                        sourceAccount = Account("CreditCard", "1234-5678-9012-3456"),
                        targetAccount = Account("BankAccount", "GB98MIDL07009312345678"),
                        amount = Amount(BigDecimal("250.50"), "USD"),
                        valuationTimestamp = Instant.parse("2023-01-02T12:00:00Z"),
                        purpose = "International transfer"
                    )
                ),
                RestMessage.Response(
                    "/api/transactions",
                    mapOf(),
                    mapOf(),
                    null,
                    201
                )
            )
        )
    }

    fun invalidTransferRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request(
                    "/api/transfers",
                    mapOf(),
                    mapOf("Content-Type" to "application/json"),
                    """
                    {
                        "sourceAccount": {
                            "type": "",
                            "identifier": "DE123456789"
                        },
                        "targetAccount": {
                            "type": "PayPal",
                            "identifier": "user@example.com"
                        },
                        "amount": {
                            "value": -100.00,
                            "currencyCode": "EUR"
                        },
                        "valuationTimestamp": "2023-01-03T12:00:00Z",
                        "purpose": "Test transfer"
                    }
                    """.trimIndent()
                )
            ),
            Arguments.of(
                RestMessage.Request(
                    "/api/transfers",
                    mapOf(),
                    mapOf("Content-Type" to "application/json"),
                    """
                    {
                        "sourceAccount": {
                            "type": "BankAccount",
                            "identifier": "DE123456789"
                        },
                        "targetAccount": {
                            "type": "BankAccount",
                            "identifier": "DE123456789"
                        },
                        "amount": {
                            "value": 100.00,
                            "currencyCode": ""
                        },
                        "valuationTimestamp": "2023-01-04T12:00:00Z",
                        "purpose": "Invalid currency"
                    }
                    """.trimIndent()
                )
            )
        )
    }
}
