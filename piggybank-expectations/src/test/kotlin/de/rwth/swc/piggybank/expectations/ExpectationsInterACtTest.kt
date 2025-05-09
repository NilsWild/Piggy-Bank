package de.rwth.swc.piggybank.expectations

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.domain.amqp.AmqpMessage
import de.interact.domain.rest.RestMessage
import de.interact.domain.serialization.MessageDeserializer
import de.interact.domain.serialization.MessageSerializer
import de.interact.domain.serialization.SerializationConstants
import de.interact.domain.shared.Message
import de.interact.domain.shared.Protocol
import de.interact.domain.shared.ProtocolData
import de.interact.domain.testobservation.config.Configuration
import de.interact.domain.testobservation.model.IncomingInterface
import de.interact.domain.testobservation.model.MessageValue
import de.interact.domain.testobservation.model.OutgoingInterface
import de.interact.domain.testobservation.service.TestObservationManager
import de.interact.domain.testobservation.sp.ObservationControllerApi
import de.interact.domain.testobservation.sp.SimpleTestObservationContextManager
import de.interact.junit.jupiter.annotation.InterACtTest
import de.interact.test.inherently
import de.rwth.swc.piggybank.notificationservice.dto.NotificationEventDto
import de.rwth.swc.piggybank.transfergateway.domain.Account
import de.rwth.swc.piggybank.transfergateway.domain.Amount
import de.rwth.swc.piggybank.transfergateway.dto.TransferRequest
import io.kotest.matchers.string.shouldStartWith
import io.vertx.core.Vertx
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpectationsInterACtTest {
    init {
        val objectMapper = ObjectMapper()
            .registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .registerModule(com.fasterxml.jackson.module.kotlin.KotlinModule.Builder().build())
        SerializationConstants.registerMessageSerializer(MessageBodySerializer(objectMapper))
        SerializationConstants.registerMessageDeserializer(MessageBodyDeserializer(objectMapper))
        val res = Configuration.observationManager ?: TestObservationManager(
            mutableListOf(), SimpleTestObservationContextManager(), ObservationControllerApi(
                "http://localhost:8080", Vertx.vertx()
            )
        )
        Configuration.observationManager = res
    }

    @InterACtTest
    @MethodSource("notificationDebit")
    fun `should notify about debit transfer`(notification: AmqpMessage<NotificationEventDto>) {
        val transferRequest = RestMessage.Request(
            "/api/transfers",
            mapOf(),
            mapOf(),
            TransferRequest(
                sourceAccount = Account("BankAccount", "DE123456789",),
                targetAccount = Account("PayPal", "user@example.com"),
                amount = Amount(BigDecimal("100.00"), "EUR"),
                valuationTimestamp = Instant.parse("2023-01-01T12:00:00Z"),
                purpose = "Test transfer"
            )
        )
        Configuration.observationManager!!.getCurrentTestCase().observedBehavior.addComponentResponse(
            MessageValue(
                SerializationConstants.mapper.writeValueAsString(transferRequest)
            ),
            OutgoingInterface(
                Protocol("REST"),
                ProtocolData(
                    mapOf(
                        "path" to "/api/transfers",
                        "method" to "POST",
                        "request" to "true"
                    )
                )
            )
        )
        Configuration.observationManager!!.getCurrentTestCase().observedBehavior.addEnvironmentResponse(
            MessageValue(
                SerializationConstants.mapper.writeValueAsString(notification)
            ),
            IncomingInterface(
                Protocol("AMQP"),
                ProtocolData(
                    mapOf(
                        "queueBindings" to "[{\"source\":\"piggybank.notifications\",\"routingKey\":\"notification.created\",\"arguments\":{}}]"
                    )
                )
            )
        )

        inherently {
            notification.body.message.shouldStartWith("You just sent 100,00 EUR")
        }
    }

    @InterACtTest
    @MethodSource("notificationCredit")
    fun `should notify about credit transfer`(notification: AmqpMessage<NotificationEventDto>) {
        val transferRequest = RestMessage.Request(
            "/api/transfers",
            mapOf(),
            mapOf(),
            TransferRequest(
                sourceAccount = Account("BankAccount", "DE123456789",),
                targetAccount = Account("PayPal", "user@example.com"),
                amount = Amount(BigDecimal("100.00"), "EUR"),
                valuationTimestamp = Instant.parse("2023-01-01T12:00:00Z"),
                purpose = "Test transfer"
            )
        )
        Configuration.observationManager!!.getCurrentTestCase().observedBehavior.addComponentResponse(
            MessageValue(
                SerializationConstants.mapper.writeValueAsString(transferRequest)
            ),
            OutgoingInterface(
                Protocol("REST"),
                ProtocolData(
                    mapOf(
                        "path" to "/api/transfers",
                        "method" to "POST",
                        "request" to "true"
                    )
                )
            )
        )
        Configuration.observationManager!!.getCurrentTestCase().observedBehavior.addEnvironmentResponse(
            MessageValue(
                SerializationConstants.mapper.writeValueAsString(notification)
            ),
            IncomingInterface(
                Protocol("AMQP"),
                ProtocolData(
                    mapOf(
                        "queueBindings" to "[{\"source\":\"piggybank.notifications\",\"routingKey\":\"notification.created\",\"arguments\":{}}]"
                    )
                )
            )
        )

        inherently {
            notification.body.message.shouldStartWith("You just received 100,00 EUR")
        }
    }

    fun notificationCredit() = listOf(
        Arguments.of(
            AmqpMessage(
                emptyMap(),
                NotificationEventDto(
                    UUID.fromString("d1a1015d-8651-4122-be9c-4bd02a078e39").toString(),
                    UUID.fromString("a9fd91dc-9a60-490a-9eb3-3812fe2c7d48").toString(),
                    "BALANCE_UPDATE",
                    "You just received 100,00 EUR",
                    false,
                    Instant.now(Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC")))
                )
            )
        )
    )

    fun notificationDebit() = listOf(
        Arguments.of(
            AmqpMessage(
                emptyMap(),
                NotificationEventDto(
                    UUID.fromString("d1a1015d-8651-4122-be9c-4bd02a078e39").toString(),
                    UUID.fromString("a9fd91dc-9a60-490a-9eb3-3812fe2c7d48").toString(),
                    "BALANCE_UPDATE",
                    "You just sent 100,00 EUR",
                    false,
                    Instant.now(Clock.fixed(Instant.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC")))
                )
            )
        )
    )


    class MessageBodyDeserializer(private val mapper: ObjectMapper, override val order: Int = Integer.MIN_VALUE):
        MessageDeserializer {

        override fun readBody(value: Message<String>, bodyType: JavaType): Any {
            return mapper.readValue(value.body, bodyType)
        }

        override fun canHandle(message: Message<String>): Boolean {
            return message is RestMessage || message is AmqpMessage
        }

    }

    class MessageBodySerializer(private val mapper: ObjectMapper, override val order: Int = Integer.MIN_VALUE):
        MessageSerializer {

        override fun canHandle(message: Message<*>): Boolean {
            return message is RestMessage || message is AmqpMessage
        }

        override fun writeBodyAsString(value: Message<*>): String {
            val res = if(value.body is String) {
                value.body as String
            }else {
                mapper.writeValueAsString(value.body)
            }
            return res
        }

    }
}
