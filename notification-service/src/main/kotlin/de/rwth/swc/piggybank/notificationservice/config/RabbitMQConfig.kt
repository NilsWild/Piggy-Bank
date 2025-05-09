package de.rwth.swc.piggybank.notificationservice.config

import com.fasterxml.jackson.databind.ObjectMapper
import de.rwth.swc.piggybank.notificationservice.service.RabbitMQService
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
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
     * Creates a topic exchange for goal events (to listen to).
     *
     * @return The topic exchange
     */
    @Bean
    fun goalExchange(): TopicExchange {
        return TopicExchange(RabbitMQService.GOAL_EXCHANGE_NAME)
    }

    /**
     * Creates a queue for goal updated events.
     *
     * @return The queue
     */
    @Bean
    fun goalUpdatedQueue(): Queue {
        return Queue("piggybank.goals.updated.notifications.queue")
    }

    /**
     * Creates a queue for goal achieved events.
     *
     * @return The queue
     */
    @Bean
    fun goalAchievedQueue(): Queue {
        return Queue("piggybank.goals.achieved.notifications.queue")
    }

    /**
     * Creates a queue for goal failed events.
     *
     * @return The queue
     */
    @Bean
    fun goalFailedQueue(): Queue {
        return Queue("piggybank.goals.failed.notifications.queue")
    }

    /**
     * Creates a binding between the goal updated queue and the goal exchange.
     *
     * @param goalUpdatedQueue The goal updated queue
     * @param goalExchange The goal exchange
     * @return The binding
     */
    @Bean
    fun goalUpdatedBinding(
        goalUpdatedQueue: Queue,
        goalExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(goalUpdatedQueue).to(goalExchange).with(RabbitMQService.GOAL_UPDATED_ROUTING_KEY)
    }

    /**
     * Creates a binding between the goal achieved queue and the goal exchange.
     *
     * @param goalAchievedQueue The goal achieved queue
     * @param goalExchange The goal exchange
     * @return The binding
     */
    @Bean
    fun goalAchievedBinding(
        goalAchievedQueue: Queue,
        goalExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(goalAchievedQueue).to(goalExchange).with(RabbitMQService.GOAL_ACHIEVED_ROUTING_KEY)
    }

    /**
     * Creates a binding between the goal failed queue and the goal exchange.
     *
     * @param goalFailedQueue The goal failed queue
     * @param goalExchange The goal exchange
     * @return The binding
     */
    @Bean
    fun goalFailedBinding(
        goalFailedQueue: Queue,
        goalExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(goalFailedQueue).to(goalExchange).with(RabbitMQService.GOAL_FAILED_ROUTING_KEY)
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
