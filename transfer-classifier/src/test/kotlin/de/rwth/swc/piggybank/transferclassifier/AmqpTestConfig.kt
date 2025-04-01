package de.rwth.swc.piggybank.transferclassifier

import de.rwth.swc.piggybank.transferclassifier.config.RabbitMQConfig
import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {

    companion object {
        const val TEST_QUEUE_NAME = "test_queue"
    }

    @Bean
    fun testClassificationExchange(): TopicExchange {
        return TopicExchange(RabbitMQConfig.CLASSIFICATION_EXCHANGE_NAME)
    }

    @Bean
    fun testDeclarables(testClassificationExchange: TopicExchange): Declarables {
        val testQueue = QueueBuilder.durable(TEST_QUEUE_NAME).build()
        return Declarables(
            testQueue,
            BindingBuilder.bind(testQueue).to(testClassificationExchange).with("#")
        )
    }
}