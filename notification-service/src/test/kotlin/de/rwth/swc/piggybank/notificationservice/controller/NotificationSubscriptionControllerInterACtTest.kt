package de.rwth.swc.piggybank.notificationservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.rest.RestMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.rest.TestRestClient
import de.rwth.swc.piggybank.notificationservice.AmqpTestConfig
import de.rwth.swc.piggybank.notificationservice.config.InterACtConfig
import de.rwth.swc.piggybank.notificationservice.config.RabbitMQTestConfig
import de.rwth.swc.piggybank.notificationservice.config.TestClockConfig
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.domain.NotificationSubscription
import de.rwth.swc.piggybank.notificationservice.dto.SubscriptionRequest
import de.rwth.swc.piggybank.notificationservice.dto.SubscriptionResponse
import de.rwth.swc.piggybank.notificationservice.repository.NotificationSubscriptionRepository
import de.rwth.swc.piggybank.notificationservice.service.NotificationService
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
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.stream.Stream

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class, TestClockConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [RabbitMQTestConfig.Initializer::class])
class NotificationSubscriptionControllerInterACtTest {

    @LocalServerPort
    private lateinit var port: Number

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var subscriptionRepository: NotificationSubscriptionRepository

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var testClient: TestRestClient

    @BeforeEach
    fun setUp() {
        testClient = InterACtConfig.testRestClient(webClientBuilder, port)
    }

    @InterACtTest
    @MethodSource("createSubscriptionRequests")
    fun `should create subscription`(requestStimulus: RestMessage.Request<SubscriptionRequest>) {
        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.CREATED

        // Parse the response body
        val responseBody = objectMapper.readValue(response.body, SubscriptionResponse::class.java)

        // Verify the response body
        responseBody.id shouldNotBe null
        responseBody.accountId shouldBe requestStimulus.body!!.accountId
        responseBody.eventType shouldBe requestStimulus.body!!.eventType
        responseBody.active shouldBe true
        responseBody.createdAt shouldNotBe null
    }

    @InterACtTest
    @MethodSource("getAllSubscriptionsRequests")
    fun `should get all subscriptions`(requestStimulus: RestMessage.Request<Void>) {
        // Create test subscriptions
        val subscription1 = createTestSubscription("account-123", NotificationEventType.BALANCE_UPDATE)
        val subscription2 = createTestSubscription("account-456", NotificationEventType.ACCOUNT_CREATED)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body
        val responseBody = objectMapper.readValue(response.body, Array<SubscriptionResponse>::class.java).toList()

        // Verify the response body
        responseBody.size shouldBe 2
        responseBody.any { it.accountId == subscription1.accountId && it.eventType == subscription1.eventType } shouldBe true
        responseBody.any { it.accountId == subscription2.accountId && it.eventType == subscription2.eventType } shouldBe true
    }

    @InterACtTest
    @MethodSource("getAccountSubscriptionsRequests")
    fun `should get account subscriptions`(requestStimulus: RestMessage.Request<Void>) {
        // Create test subscriptions for the account
        val accountId = "account-123"
        createTestSubscription(accountId, NotificationEventType.BALANCE_UPDATE)
        createTestSubscription(accountId, NotificationEventType.ACCOUNT_CREATED)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body
        val responseBody = objectMapper.readValue(response.body, Array<SubscriptionResponse>::class.java).toList()

        // Verify the response body
        responseBody.size shouldBe 2
        responseBody.all { it.accountId == accountId } shouldBe true
        responseBody.any { it.eventType == NotificationEventType.BALANCE_UPDATE } shouldBe true
        responseBody.any { it.eventType == NotificationEventType.ACCOUNT_CREATED } shouldBe true
    }

    @InterACtTest
    @MethodSource("deactivateSubscriptionRequests")
    fun `should deactivate subscription`(requestStimulus: RestMessage.Request<Void>) {
        // Create a test subscription
        val subscriptionId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        createTestSubscription("account-123", NotificationEventType.BALANCE_UPDATE, subscriptionId)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.DELETE, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Autowired
    private lateinit var clock: Clock

    private fun createTestSubscription(
        accountId: String,
        eventType: NotificationEventType,
        id: UUID = UUID.randomUUID()
    ): NotificationSubscription {
        val subscription = NotificationSubscription(
            id = id,
            accountId = accountId,
            eventType = eventType,
            active = true,
            createdAt = Instant.now(clock)
        )
        return subscriptionRepository.save(subscription)
    }

    fun createSubscriptionRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request(
                    "/api/subscriptions",
                    mapOf("Content-Type" to "application/json"),
                    mapOf(),
                    SubscriptionRequest(
                        accountId = "account-123",
                        eventType = NotificationEventType.BALANCE_UPDATE
                    )
                )
            ),
            Arguments.of(
                RestMessage.Request(
                    "/api/subscriptions",
                    mapOf("Content-Type" to "application/json"),
                    mapOf(),
                    SubscriptionRequest(
                        accountId = "account-456",
                        eventType = NotificationEventType.ACCOUNT_CREATED
                    )
                )
            )
        )
    }

    fun getAllSubscriptionsRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/subscriptions",
                    mapOf(),
                    mapOf(),
                    null
                )
            )
        )
    }

    fun getAccountSubscriptionsRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/subscriptions/account/account-123",
                    mapOf(),
                    mapOf(),
                    null
                )
            )
        )
    }

    fun deactivateSubscriptionRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/subscriptions/00000000-0000-0000-0000-000000000001",
                    mapOf(),
                    mapOf(),
                    null
                )
            )
        )
    }
}
