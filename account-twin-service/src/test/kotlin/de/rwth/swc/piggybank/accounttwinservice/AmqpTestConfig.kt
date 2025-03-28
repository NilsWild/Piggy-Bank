package de.rwth.swc.piggybank.accounttwinservice

import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {

    @Bean
    fun testDeclarables(accountExchange: TopicExchange): Declarables {
        val testQueue = QueueBuilder.durable("test_queue").build()
        return Declarables(
            testQueue,
            BindingBuilder.bind(testQueue).to(accountExchange).with("#"))
    }
}
