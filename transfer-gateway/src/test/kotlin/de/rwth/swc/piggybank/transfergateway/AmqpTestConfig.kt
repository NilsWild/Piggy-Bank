package de.rwth.swc.piggybank.transfergateway

import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {

    @Bean
    fun testTransferExchange(): TopicExchange {
        return TopicExchange("piggybank.transfers")
    }

    @Bean
    fun testDeclarables(testTransferExchange: TopicExchange): Declarables {
        val testQueue = QueueBuilder.durable("test_queue").build()
        return Declarables(
            testQueue,
            BindingBuilder.bind(testQueue).to(testTransferExchange).with("#"))
    }
}
