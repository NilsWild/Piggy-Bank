package de.rwth.swc.piggybank.notificationservice.config

import de.rwth.swc.piggybank.notificationservice.service.RabbitMQService
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
     * Creates a topic exchange for account events (to listen to).
     *
     * @return The topic exchange
     */
    @Bean
    fun accountExchange(): TopicExchange {
        return TopicExchange("piggybank.accounts")
    }

    /**
     * Creates a topic exchange for notification events (to publish to).
     *
     * @return The topic exchange
     */
    @Bean
    fun notificationExchange(): TopicExchange {
        return TopicExchange(RabbitMQService.NOTIFICATION_EXCHANGE_NAME)
    }

    /**
     * Creates a queue for account updated events.
     *
     * @return The queue
     */
    @Bean
    fun accountUpdatedQueue(): Queue {
        return Queue("piggybank.accounts.updated.notifications.queue")
    }

    /**
     * Creates a queue for notification events.
     *
     * @return The queue
     */
    @Bean
    fun notificationQueue(): Queue {
        return Queue("${RabbitMQService.NOTIFICATION_EXCHANGE_NAME}.queue")
    }

    /**
     * Creates a binding between the account updated queue and the account exchange.
     *
     * @param accountUpdatedQueue The account updated queue
     * @param accountExchange The account exchange
     * @return The binding
     */
    @Bean
    fun accountUpdatedBinding(
        accountUpdatedQueue: Queue,
        accountExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(accountUpdatedQueue).to(accountExchange).with("account.updated")
    }

    /**
     * Creates a binding between the notification queue and the notification exchange.
     *
     * @param notificationQueue The notification queue
     * @param notificationExchange The notification exchange
     * @return The binding
     */
    @Bean
    fun notificationBinding(
        notificationQueue: Queue,
        notificationExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(RabbitMQService.NOTIFICATION_ROUTING_KEY)
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