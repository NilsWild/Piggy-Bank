package de.rwth.swc.piggybank.notificationservice

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import de.interact.domain.amqp.AmqpMessage
import de.interact.domain.rest.RestMessage
import de.interact.domain.serialization.MessageDeserializer
import de.interact.domain.serialization.MessageSerializer
import de.interact.domain.serialization.SerializationConstants
import de.interact.domain.shared.Message
import de.interact.domain.testobservation.config.Configuration
import de.interact.domain.testobservation.service.TestObservationManager
import de.interact.domain.testobservation.sp.ObservationControllerApi
import de.interact.domain.testobservation.sp.SimpleTestObservationContextManager
import de.interact.rest.TestRestClient
import de.interact.rest.observer.WebClientObserver
import io.vertx.core.Vertx
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
@ComponentScan("de.interact")
class InterACtConfig {

    @Bean
    fun observationManager(objectMapper: ObjectMapper): TestObservationManager {
        SerializationConstants.registerMessageSerializer(MessageBodySerializer(objectMapper))
        SerializationConstants.registerMessageDeserializer(MessageBodyDeserializer(objectMapper))
        val res = Configuration.observationManager ?: TestObservationManager(
            mutableListOf(), SimpleTestObservationContextManager(), ObservationControllerApi(
                "http://localhost:8080", Vertx.vertx()
            )
        )
        Configuration.observationManager = res
        return res
    }

    @Bean
    fun webClientObserverForComponent(): WebClientObserver {
        return WebClientObserver()
    }

    @Bean
    fun testWebClientCustomizer(observer: WebClientObserver): WebClientCustomizer {
        return WebClientCustomizer { builder: WebClient.Builder ->
            builder.filter(observer)
        }
    }

    companion object {
        fun testRestClient(builder: WebClient.Builder, port: Number): TestRestClient {
            val webClient = builder.filters { filters ->
                filters.removeAll(filters.filterIsInstance<WebClientObserver>())
            }.baseUrl("http://localhost:$port").build()
            return TestRestClient(webClient)
        }
    }

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
            } else {
                mapper.writeValueAsString(value.body)
            }
            return res
        }
    }
}