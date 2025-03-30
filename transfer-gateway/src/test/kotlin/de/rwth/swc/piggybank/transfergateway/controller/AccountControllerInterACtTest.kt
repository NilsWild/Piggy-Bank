package de.rwth.swc.piggybank.transfergateway.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.rest.RestMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.rest.TestRestClient
import de.rwth.swc.piggybank.transfergateway.AmqpTestConfig
import de.rwth.swc.piggybank.transfergateway.InterACtConfig
import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.dto.AccountRequest
import de.rwth.swc.piggybank.transfergateway.service.AccountService
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.amqp.rabbit.core.RabbitTemplate
import de.rwth.swc.piggybank.transfergateway.config.MockServerConfig
import de.rwth.swc.piggybank.transfergateway.config.RabbitMQTestConfig
import java.util.stream.Stream

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [RabbitMQTestConfig.Initializer::class, MockServerConfig.Initializer::class])
class AccountControllerInterACtTest {

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
    @MethodSource("getAllMonitoredAccountsRequests")
    fun `should get all monitored accounts`(requestStimulus: RestMessage.Request<Void>) {
        // Add some test accounts
        val account1 = Account("BankAccount", "DE123456789")
        val account2 = Account("PayPal", "user@example.com")
        accountService.addMonitoredAccount(account1)
        accountService.addMonitoredAccount(account2)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body to a Set of Account objects
        val responseBody = objectMapper.readValue(response.body, Array<Account>::class.java).toSet()

        // Verify the response body contains the expected accounts
        responseBody shouldContain account1
        responseBody shouldContain account2
        responseBody.size shouldBe 2
    }

    @InterACtTest
    @MethodSource("addMonitoredAccountRequests")
    fun `should add monitored account`(requestStimulus: RestMessage.Request<AccountRequest>) {
        val accountRequest = requestStimulus.body!!

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.CREATED

        // Verify the account was added to the service
        val allAccounts = accountService.getAllMonitoredAccounts()
        allAccounts shouldContain accountRequest.account
    }

    @InterACtTest
    @MethodSource("addMonitoredAccountRequests")
    fun `should return conflict when adding existing account`(requestStimulus: RestMessage.Request<AccountRequest>) {
        val accountRequest = requestStimulus.body!!

        // Add the account first
        accountService.addMonitoredAccount(accountRequest.account)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.CONFLICT
    }

    @InterACtTest
    @MethodSource("removeMonitoredAccountRequests")
    fun `should remove monitored account`(requestStimulus: RestMessage.Request<AccountRequest>) {
        val accountRequest = requestStimulus.body!!

        // Add the account first
        accountService.addMonitoredAccount(accountRequest.account)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.DELETE, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.NO_CONTENT

        // Verify the account was removed from the service
        val allAccounts = accountService.getAllMonitoredAccounts()
        allAccounts.contains(accountRequest.account) shouldBe false
    }

    @InterACtTest
    @MethodSource("removeMonitoredAccountRequests")
    fun `should return not found when removing non-existent account`(requestStimulus: RestMessage.Request<AccountRequest>) {

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.DELETE, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    fun getAllMonitoredAccountsRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/monitored-accounts",
                    mapOf(),
                    mapOf(),
                    null
                )
            )
        )
    }

    fun addMonitoredAccountRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request(
                    "/api/monitored-accounts",
                    mapOf(),
                    mapOf(),
                    AccountRequest(
                        account = Account(
                            type = "BankAccount",
                            identifier = "DE123456789"
                        )
                    )
                )
            ),
        )
    }

    fun removeMonitoredAccountRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request(
                    "/api/monitored-accounts",
                    mapOf(),
                    mapOf(),
                    AccountRequest(
                        account = Account(
                            type = "BankAccount",
                            identifier = "DE123456789"
                        )
                    )
                )
            ),
        )
    }
}
