package de.rwth.swc.piggybank.goalservice.config

import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {
    @Bean
    fun testDeclarables(
        classificationExchange: TopicExchange,
        goalExchange: TopicExchange
    ): Declarables {
        val testQueue = QueueBuilder.durable(RabbitMQTestConfig.TEST_QUEUE_NAME).build()
        
        return Declarables(
            testQueue,
            // Bind test queue to classification exchange
            BindingBuilder.bind(testQueue).to(classificationExchange).with(RabbitMQConfig.CLASSIFICATION_ROUTING_KEY),
            // Bind test queue to goal exchange for all goal events
            BindingBuilder.bind(testQueue).to(goalExchange).with(RabbitMQConfig.GOAL_UPDATED_ROUTING_KEY),
            BindingBuilder.bind(testQueue).to(goalExchange).with(RabbitMQConfig.GOAL_ACHIEVED_ROUTING_KEY),
            BindingBuilder.bind(testQueue).to(goalExchange).with(RabbitMQConfig.GOAL_FAILED_ROUTING_KEY)
        )
    }
}