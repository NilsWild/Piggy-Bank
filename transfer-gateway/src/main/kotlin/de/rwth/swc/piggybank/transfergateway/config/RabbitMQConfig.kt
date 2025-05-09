package de.rwth.swc.piggybank.transfergateway.config

import com.fasterxml.jackson.databind.ObjectMapper
import de.rwth.swc.piggybank.transfergateway.service.RabbitMQService
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for RabbitMQ.
 */
@Configuration
class RabbitMQConfig {

    /**
     * Creates a topic exchange for transfer events.
     *
     * @return The topic exchange
     */
    @Bean
    fun transferExchange(): TopicExchange {
        return TopicExchange(RabbitMQService.EXCHANGE_NAME)
    }

    /**
     * Creates a queue for transfer events.
     *
     * @return The queue
     */
    @Bean
    fun transferQueue(): Queue {
        return Queue("${RabbitMQService.EXCHANGE_NAME}.queue")
    }

    /**
     * Creates a binding between the queue and the exchange.
     *
     * @param queue The queue
     * @param exchange The exchange
     * @return The binding
     */
    @Bean
    fun binding(@Qualifier("transferQueue") queue: Queue, @Qualifier("transferExchange") exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(RabbitMQService.ROUTING_KEY)
    }

    /**
     * Creates a Jackson2JsonMessageConverter for converting objects to JSON.
     *
     * @return The message converter
     */
    @Bean
    fun messageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

    /**
    * Configures the RabbitTemplate with the message converter.
    *
    * @param messageConverter The message converter
    * @return The RabbitTemplateCustomizer
    */
    @Bean
    fun rabbitTemplateMessageConverterCustomizer(messageConverter: Jackson2JsonMessageConverter): RabbitTemplateCustomizer {
        return RabbitTemplateCustomizer { template -> template.messageConverter = messageConverter }
    }
}
