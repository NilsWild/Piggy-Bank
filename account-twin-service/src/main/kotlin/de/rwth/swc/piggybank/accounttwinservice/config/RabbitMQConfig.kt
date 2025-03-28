package de.rwth.swc.piggybank.accounttwinservice.config

import de.rwth.swc.piggybank.accounttwinservice.service.RabbitMQService
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for RabbitMQ.
 */
@Configuration
class RabbitMQConfig {

    /**
     * Creates a topic exchange for account events.
     *
     * @return The topic exchange
     */
    @Bean
    fun accountExchange(): TopicExchange {
        return TopicExchange(RabbitMQService.EXCHANGE_NAME)
    }

    /**
     * Creates a queue for account created events.
     *
     * @return The queue
     */
    @Bean
    fun accountCreatedQueue(): Queue {
        return Queue("${RabbitMQService.EXCHANGE_NAME}.created.queue")
    }

    /**
     * Creates a queue for account updated events.
     *
     * @return The queue
     */
    @Bean
    fun accountUpdatedQueue(): Queue {
        return Queue("${RabbitMQService.EXCHANGE_NAME}.updated.queue")
    }

    /**
     * Creates a queue for account deleted events.
     *
     * @return The queue
     */
    @Bean
    fun accountDeletedQueue(): Queue {
        return Queue("${RabbitMQService.EXCHANGE_NAME}.deleted.queue")
    }

    /**
     * Creates a binding between the account created queue and the exchange.
     *
     * @param accountCreatedQueue The account created queue
     * @param accountExchange The exchange
     * @return The binding
     */
    @Bean
    fun accountCreatedBinding(
        accountCreatedQueue: Queue,
        accountExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(accountCreatedQueue).to(accountExchange).with(RabbitMQService.ACCOUNT_CREATED_ROUTING_KEY)
    }

    /**
     * Creates a binding between the account updated queue and the exchange.
     *
     * @param accountUpdatedQueue The account updated queue
     * @param accountExchange The exchange
     * @return The binding
     */
    @Bean
    fun accountUpdatedBinding(
        accountUpdatedQueue: Queue,
        accountExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(accountUpdatedQueue).to(accountExchange).with(RabbitMQService.ACCOUNT_UPDATED_ROUTING_KEY)
    }

    /**
     * Creates a binding between the account deleted queue and the exchange.
     *
     * @param accountDeletedQueue The account deleted queue
     * @param accountExchange The exchange
     * @return The binding
     */
    @Bean
    fun accountDeletedBinding(
        accountDeletedQueue: Queue,
        accountExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(accountDeletedQueue).to(accountExchange).with(RabbitMQService.ACCOUNT_DELETED_ROUTING_KEY)
    }

    /**
     * Creates a Jackson2JsonMessageConverter for converting objects to JSON.
     *
     * @return The message converter
     */
    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter()
    }

    /**
     * Configures the RabbitTemplate with the message converter.
     *
     * @param connectionFactory The connection factory
     * @param messageConverter The message converter
     * @return The configured RabbitTemplate
     */
    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: Jackson2JsonMessageConverter
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = messageConverter
        return rabbitTemplate
    }
}
