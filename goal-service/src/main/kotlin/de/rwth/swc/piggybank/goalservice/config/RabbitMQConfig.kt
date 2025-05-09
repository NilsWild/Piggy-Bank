package de.rwth.swc.piggybank.goalservice.config

import com.fasterxml.jackson.databind.ObjectMapper
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

    companion object {
        // Exchange and queue for receiving account updates
        const val ACCOUNT_EXCHANGE_NAME = "piggybank.accounts"
        const val ACCOUNT_UPDATED_ROUTING_KEY = "account.updated"
        const val ACCOUNT_UPDATED_QUEUE_NAME = "piggybank.accounts.updated.goal-service"

        // Exchange and queue for receiving classification results
        const val CLASSIFICATION_EXCHANGE_NAME = "piggybank.classifications"
        const val CLASSIFICATION_ROUTING_KEY = "classification.event"
        const val CLASSIFICATION_QUEUE_NAME = "piggybank.classifications.goal-service"

        // Exchange and queue for sending goal updates
        const val GOAL_EXCHANGE_NAME = "piggybank.goals"
        const val GOAL_UPDATED_ROUTING_KEY = "goal.updated"
        const val GOAL_ACHIEVED_ROUTING_KEY = "goal.achieved"
        const val GOAL_FAILED_ROUTING_KEY = "goal.failed"
    }

    /**
     * Creates a topic exchange for receiving account updates.
     */
    @Bean
    fun accountExchange(): TopicExchange {
        return TopicExchange(ACCOUNT_EXCHANGE_NAME)
    }

    /**
     * Creates a queue for receiving account updated events.
     */
    @Bean
    fun accountUpdatedQueue(): Queue {
        return Queue(ACCOUNT_UPDATED_QUEUE_NAME)
    }

    /**
     * Creates a binding between the account updated queue and the account exchange.
     */
    @Bean
    fun accountUpdatedBinding(
        accountUpdatedQueue: Queue,
        accountExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(accountUpdatedQueue).to(accountExchange).with(ACCOUNT_UPDATED_ROUTING_KEY)
    }

    /**
     * Creates a topic exchange for receiving classification results.
     */
    @Bean
    fun classificationExchange(): TopicExchange {
        return TopicExchange(CLASSIFICATION_EXCHANGE_NAME)
    }

    /**
     * Creates a queue for receiving classification events.
     */
    @Bean
    fun classificationQueue(): Queue {
        return Queue(CLASSIFICATION_QUEUE_NAME)
    }

    /**
     * Creates a binding between the classification queue and the classification exchange.
     */
    @Bean
    fun classificationBinding(
        classificationQueue: Queue,
        classificationExchange: TopicExchange
    ): Binding {
        return BindingBuilder.bind(classificationQueue).to(classificationExchange).with(CLASSIFICATION_ROUTING_KEY)
    }

    /**
     * Creates a topic exchange for sending goal updates.
     */
    @Bean
    fun goalExchange(): TopicExchange {
        return TopicExchange(GOAL_EXCHANGE_NAME)
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