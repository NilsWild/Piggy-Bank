package de.rwth.swc.piggybank.goalservice.config

import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {

    @Bean
    fun testAccountExchange(): TopicExchange {
        return TopicExchange(RabbitMQConfig.ACCOUNT_EXCHANGE_NAME)
    }

    @Bean
    fun testClassificationExchange(): TopicExchange {
        return TopicExchange(RabbitMQConfig.CLASSIFICATION_EXCHANGE_NAME)
    }

    @Bean
    fun testGoalExchange(): TopicExchange {
        return TopicExchange(RabbitMQConfig.GOAL_EXCHANGE_NAME)
    }

    @Bean
    fun testDeclarables(
        testAccountExchange: TopicExchange,
        testClassificationExchange: TopicExchange,
        testGoalExchange: TopicExchange
    ): Declarables {
        val testQueue = QueueBuilder.durable(RabbitMQTestConfig.TEST_QUEUE_NAME).build()
        
        return Declarables(
            testQueue,
            // Bind test queue to account exchange
            BindingBuilder.bind(testQueue).to(testAccountExchange).with(RabbitMQConfig.ACCOUNT_UPDATED_ROUTING_KEY),
            // Bind test queue to classification exchange
            BindingBuilder.bind(testQueue).to(testClassificationExchange).with(RabbitMQConfig.CLASSIFICATION_ROUTING_KEY),
            // Bind test queue to goal exchange for all goal events
            BindingBuilder.bind(testQueue).to(testGoalExchange).with(RabbitMQConfig.GOAL_UPDATED_ROUTING_KEY),
            BindingBuilder.bind(testQueue).to(testGoalExchange).with(RabbitMQConfig.GOAL_ACHIEVED_ROUTING_KEY),
            BindingBuilder.bind(testQueue).to(testGoalExchange).with(RabbitMQConfig.GOAL_FAILED_ROUTING_KEY)
        )
    }
}