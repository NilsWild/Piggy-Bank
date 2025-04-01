package de.rwth.swc.piggybank.transferclassifier.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.amqp.observer.SpringAMQPInterACtObserverConfiguration
import de.interact.domain.amqp.AmqpMessage
import de.interact.junit.jupiter.annotation.InterACtTest
import de.rwth.swc.piggybank.transferclassifier.AmqpTestConfig
import de.rwth.swc.piggybank.transferclassifier.InterACtConfig
import de.rwth.swc.piggybank.transferclassifier.config.RabbitMQConfig
import de.rwth.swc.piggybank.transferclassifier.config.RabbitMQTestConfig
import de.rwth.swc.piggybank.transferclassifier.domain.Account
import de.rwth.swc.piggybank.transferclassifier.domain.Amount
import de.rwth.swc.piggybank.transferclassifier.domain.ClassificationResult
import de.rwth.swc.piggybank.transferclassifier.domain.Transfer
import de.rwth.swc.piggybank.transferclassifier.util.RabbitMQTestUtils
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

@SpringBootTest
@ActiveProfiles("test")
@Import(InterACtConfig::class, AmqpTestConfig::class, SpringAMQPInterACtObserverConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [RabbitMQTestConfig.Initializer::class])
class TransferClassifierServiceInterACtTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @BeforeEach
    fun setUp() {
        // Clear any existing messages in the queues
        while (rabbitTemplate.receive(RabbitMQConfig.TRANSFER_QUEUE_NAME) != null) {
            // Do nothing, just drain the queue
        }
        while (rabbitTemplate.receive(RabbitMQTestConfig.TEST_QUEUE_NAME) != null) {
            // Do nothing, just drain the queue
        }
    }

    @InterACtTest
    @ParameterizedTest
    @MethodSource("transferWithGroceryKeywords")
    fun `should classify transfer with grocery keywords`(transferStimulus: AmqpMessage<Transfer>) {
        val transfer = transferStimulus.body

        // Send the transfer to the queue
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
            RabbitMQConfig.TRANSFER_ROUTING_KEY,
            transfer
        )

        // Wait for the classification result
        val message = RabbitMQTestUtils.waitForMessage(
            rabbitTemplate,
            RabbitMQTestConfig.TEST_QUEUE_NAME,
            2,
            TimeUnit.SECONDS
        )

        // Verify the classification result
        message shouldNotBe null
        val messageBody = String(message!!.body)
        val classificationResult = objectMapper.readValue(messageBody, ClassificationResult::class.java)

        classificationResult.transferId shouldBe transfer.id
        classificationResult.classifications shouldContain "Grocery"
    }

    @InterACtTest
    @ParameterizedTest
    @MethodSource("transferWithHolidayKeywords")
    fun `should classify transfer with holiday keywords`(transferStimulus: AmqpMessage<Transfer>) {
        val transfer = transferStimulus.body

        // Send the transfer to the queue
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
            RabbitMQConfig.TRANSFER_ROUTING_KEY,
            transfer
        )

        // Wait for the classification result
        val message = RabbitMQTestUtils.waitForMessage(
            rabbitTemplate,
            RabbitMQTestConfig.TEST_QUEUE_NAME,
            2,
            TimeUnit.SECONDS
        )

        // Verify the classification result
        message shouldNotBe null
        val messageBody = String(message!!.body)
        val classificationResult = objectMapper.readValue(messageBody, ClassificationResult::class.java)

        classificationResult.transferId shouldBe transfer.id
        classificationResult.classifications shouldContain "Holiday"
    }

    @InterACtTest
    @ParameterizedTest
    @MethodSource("transferWithBothKeywords")
    fun `should classify transfer with both grocery and holiday keywords`(transferStimulus: AmqpMessage<Transfer>) {
        val transfer = transferStimulus.body

        // Send the transfer to the queue
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
            RabbitMQConfig.TRANSFER_ROUTING_KEY,
            transfer
        )

        // Wait for the classification result
        val message = RabbitMQTestUtils.waitForMessage(
            rabbitTemplate,
            RabbitMQTestConfig.TEST_QUEUE_NAME,
            2,
            TimeUnit.SECONDS
        )

        // Verify the classification result
        message shouldNotBe null
        val messageBody = String(message!!.body)
        val classificationResult = objectMapper.readValue(messageBody, ClassificationResult::class.java)

        classificationResult.transferId shouldBe transfer.id
        classificationResult.classifications shouldContainExactlyInAnyOrder listOf("Grocery", "Holiday")
    }

    @InterACtTest
    @ParameterizedTest
    @MethodSource("transferWithoutKeywords")
    fun `should classify transfer without keywords`(transferStimulus: AmqpMessage<Transfer>) {
        val transfer = transferStimulus.body

        // Send the transfer to the queue
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
            RabbitMQConfig.TRANSFER_ROUTING_KEY,
            transfer
        )

        // Wait for the classification result
        val message = RabbitMQTestUtils.waitForMessage(
            rabbitTemplate,
            RabbitMQTestConfig.TEST_QUEUE_NAME,
            2,
            TimeUnit.SECONDS
        )

        // Verify the classification result
        message shouldNotBe null
        val messageBody = String(message!!.body)
        val classificationResult = objectMapper.readValue(messageBody, ClassificationResult::class.java)

        classificationResult.transferId shouldBe transfer.id
        classificationResult.classifications shouldBe emptyList()
    }

    fun transferWithGroceryKeywords(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.TRANSFER_ROUTING_KEY
                    ),
                    Transfer(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        sourceAccount = Account("IBAN", "DE123456789"),
                        targetAccount = Account("IBAN", "DE987654321"),
                        amount = Amount(BigDecimal("100.00"), "EUR"),
                        valuationTimestamp = Instant.parse("2023-01-01T12:00:00Z"),
                        purpose = "Payment to Aldi"
                    )
                )
            )
        )
    }

    fun transferWithHolidayKeywords(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.TRANSFER_ROUTING_KEY
                    ),
                    Transfer(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        sourceAccount = Account("IBAN", "DE123456789"),
                        targetAccount = Account("IBAN", "DE987654321"),
                        amount = Amount(BigDecimal("500.00"), "EUR"),
                        valuationTimestamp = Instant.parse("2023-01-02T12:00:00Z"),
                        purpose = "Payment for hotel"
                    )
                )
            )
        )
    }

    fun transferWithBothKeywords(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.TRANSFER_ROUTING_KEY
                    ),
                    Transfer(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                        sourceAccount = Account("IBAN", "DE123456789"),
                        targetAccount = Account("IBAN", "DE987654321"),
                        amount = Amount(BigDecimal("250.00"), "EUR"),
                        valuationTimestamp = Instant.parse("2023-01-03T12:00:00Z"),
                        purpose = "Payment to Lidl for vacation supplies"
                    )
                )
            )
        )
    }

    fun transferWithoutKeywords(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(
                AmqpMessage(
                    mapOf(
                        "exchange" to RabbitMQConfig.TRANSFER_EXCHANGE_NAME,
                        "routingKey" to RabbitMQConfig.TRANSFER_ROUTING_KEY
                    ),
                    Transfer(
                        id = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                        sourceAccount = Account("IBAN", "DE123456789"),
                        targetAccount = Account("IBAN", "DE987654321"),
                        amount = Amount(BigDecimal("150.00"), "EUR"),
                        valuationTimestamp = Instant.parse("2023-01-04T12:00:00Z"),
                        purpose = "Payment to restaurant"
                    )
                )
            )
        )
    }
}
