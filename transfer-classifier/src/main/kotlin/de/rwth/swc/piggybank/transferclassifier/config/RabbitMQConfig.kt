package de.rwth.swc.piggybank.transferclassifier.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for RabbitMQ.
 */
@Configuration
class RabbitMQConfig {

    companion object {
        // Exchange and queue for receiving transfers
        const val TRANSFER_EXCHANGE_NAME = "piggybank.transfers"
        const val TRANSFER_ROUTING_KEY = "transfer.event"
        const val TRANSFER_QUEUE_NAME = "piggybank.transfers.classifier"

        // Exchange and queue for sending classification results
        const val CLASSIFICATION_EXCHANGE_NAME = "piggybank.classifications"
        const val CLASSIFICATION_ROUTING_KEY = "classification.event"
    }

    /**
     * Creates a topic exchange for receiving transfers.
     */
    @Bean
    fun transferExchange(): TopicExchange {
        return TopicExchange(TRANSFER_EXCHANGE_NAME)
    }

    /**
     * Creates a queue for receiving transfers.
     */
    @Bean
    fun transferQueue(): Queue {
        return Queue(TRANSFER_QUEUE_NAME)
    }

    /**
     * Creates a binding between the transfer queue and the transfer exchange.
     */
    @Bean
    fun transferBinding(
        @Qualifier("transferQueue") queue: Queue,
        @Qualifier("transferExchange") exchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(TRANSFER_ROUTING_KEY)
    }

    /**
     * Creates a topic exchange for sending classification results.
     */
    @Bean
    fun classificationExchange(): TopicExchange {
        return TopicExchange(CLASSIFICATION_EXCHANGE_NAME)
    }

    /**
     * Creates a Jackson2JsonMessageConverter for converting objects to JSON.
     */
    @Bean
    fun messageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

    /**
     * Configures the RabbitTemplate with the message converter.
     */
    @Bean
    fun rabbitTemplateMessageConverterCustomizer(messageConverter: Jackson2JsonMessageConverter): RabbitTemplateCustomizer {
        return RabbitTemplateCustomizer { template -> template.messageConverter = messageConverter }
    }
}