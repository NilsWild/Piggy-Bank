package de.rwth.swc.piggybank.notificationservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.rest.RestMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.rest.TestRestClient
import de.rwth.swc.piggybank.notificationservice.AmqpTestConfig
import de.rwth.swc.piggybank.notificationservice.NotificationServiceApplication
import de.rwth.swc.piggybank.notificationservice.config.InterACtConfig
import de.rwth.swc.piggybank.notificationservice.config.RabbitMQTestConfig
import de.rwth.swc.piggybank.notificationservice.config.TestClockConfig
import de.rwth.swc.piggybank.notificationservice.domain.Notification
import de.rwth.swc.piggybank.notificationservice.domain.NotificationEventType
import de.rwth.swc.piggybank.notificationservice.repository.NotificationRepository
import de.rwth.swc.piggybank.notificationservice.service.NotificationService
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
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.stream.Stream

@SpringBootTest(
    classes = [NotificationServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class, TestClockConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [RabbitMQTestConfig.Initializer::class])
class NotificationControllerInterACtTest {

    @LocalServerPort
    private lateinit var port: Number

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var testClient: TestRestClient

    @BeforeEach
    fun setUp() {
        testClient = InterACtConfig.testRestClient(webClientBuilder, port)
    }

    @InterACtTest
    @MethodSource("getAllNotificationsRequests")
    fun `should get all notifications`(requestStimulus: RestMessage.Request<Void>) {
        // Create test notifications
        val notification1 = createTestNotification("Test notification 1", "account-123", false)
        val notification2 = createTestNotification("Test notification 2", "account-456", true)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body to JSON
        val jsonNode = objectMapper.readTree(response.body)

        // Verify the response body using JSON paths
        jsonNode.path("totalElements").asInt() shouldBe 2
        jsonNode.path("totalPages").asInt() shouldBe 1
        jsonNode.path("number").asInt() shouldBe 0
        jsonNode.path("size").asInt() shouldBe 10
    }

    @InterACtTest
    @MethodSource("getAccountNotificationsRequests")
    fun `should get account notifications`(requestStimulus: RestMessage.Request<Void>) {
        // Create test notifications for the account
        val accountId = "account-123"
        val notification1 = createTestNotification("Test notification 1", accountId, false)
        val notification2 = createTestNotification("Test notification 2", accountId, true)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body to JSON
        val jsonNode = objectMapper.readTree(response.body)

        // Verify the response body using JSON paths
        jsonNode.path("totalElements").asInt() shouldBe 2
        jsonNode.path("totalPages").asInt() shouldBe 1
        jsonNode.path("number").asInt() shouldBe 0
        jsonNode.path("size").asInt() shouldBe 10
    }

    @InterACtTest
    @MethodSource("getUnreadNotificationsRequests")
    fun `should get unread notifications`(requestStimulus: RestMessage.Request<Void>) {
        // Create test notifications
        createTestNotification("Test notification 1", "account-123", false)
        createTestNotification("Test notification 2", "account-456", false)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body to JSON
        val jsonNode = objectMapper.readTree(response.body)

        // Verify the response body using JSON paths
        jsonNode.path("totalElements").asInt() shouldBe 2
        jsonNode.path("totalPages").asInt() shouldBe 1
        jsonNode.path("number").asInt() shouldBe 0
        jsonNode.path("size").asInt() shouldBe 10
    }

    @InterACtTest
    @MethodSource("getUnreadNotificationsForAccountRequests")
    fun `should get unread notifications for account`(requestStimulus: RestMessage.Request<Void>) {
        // Create test notifications for the account
        val accountId = "account-123"
        createTestNotification("Test notification 1", accountId, false)
        createTestNotification("Test notification 2", accountId, false)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body to JSON
        val jsonNode = objectMapper.readTree(response.body)

        // Verify the response body using JSON paths
        jsonNode.path("totalElements").asInt() shouldBe 2
        jsonNode.path("totalPages").asInt() shouldBe 1
        jsonNode.path("number").asInt() shouldBe 0
        jsonNode.path("size").asInt() shouldBe 10
    }

    @InterACtTest
    @MethodSource("countUnreadNotificationsRequests")
    fun `should count unread notifications`(requestStimulus: RestMessage.Request<Void>) {
        // Create test notifications
        createTestNotification("Test notification 1", "account-123", false)
        createTestNotification("Test notification 2", "account-456", false)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.GET, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.OK

        // Parse the response body to JSON
        val jsonNode = objectMapper.readTree(response.body)

        // Verify the response body using JSON path
        jsonNode.path("count").asLong() shouldBe 2
    }

    @InterACtTest
    @MethodSource("markNotificationAsReadRequests")
    fun `should mark notification as read`(requestStimulus: RestMessage.Request<Void>) {
        // Create a test notification
        val notificationId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        createTestNotification("Test notification", "account-123", false, notificationId)

        // Send the request and get the response
        val response = testClient.prepare(HttpMethod.POST, requestStimulus)
            .exchangeToMono { response -> response.toEntity(String::class.java) }.block()

        // Verify the response
        response shouldNotBe null
        response!!.statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Autowired
    private lateinit var clock: Clock

    private fun createTestNotification(
        message: String,
        accountId: String,
        read: Boolean,
        id: UUID = UUID.randomUUID()
    ): Notification {
        val notification = Notification(
            id = id,
            message = message,
            accountId = accountId,
            eventType = NotificationEventType.BALANCE_UPDATE,
            read = read,
            createdAt = Instant.now(clock)
        )
        return notificationRepository.save(notification)
    }

    fun getAllNotificationsRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/notifications",
                    mapOf(),
                    mapOf("page" to "0", "size" to "10"),
                    null
                )
            )
        )
    }

    fun getAccountNotificationsRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/notifications/account/account-123",
                    mapOf(),
                    mapOf("page" to "0", "size" to "10"),
                    null
                )
            )
        )
    }

    fun getUnreadNotificationsRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/notifications/unread",
                    mapOf(),
                    mapOf("page" to "0", "size" to "10"),
                    null
                )
            )
        )
    }

    fun getUnreadNotificationsForAccountRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/notifications/account/account-123/unread",
                    mapOf(),
                    mapOf("page" to "0", "size" to "10"),
                    null
                )
            )
        )
    }

    fun countUnreadNotificationsRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/notifications/count",
                    mapOf(),
                    mapOf(),
                    null
                )
            )
        )
    }

    fun markNotificationAsReadRequests(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                RestMessage.Request<Void>(
                    "/api/notifications/00000000-0000-0000-0000-000000000001/read",
                    mapOf(),
                    mapOf(),
                    null
                )
            )
        )
    }
}
