package de.rwth.swc.piggybank.notificationservice

import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {

    @Bean
    fun testNotificationExchange(): TopicExchange {
        return TopicExchange("piggybank.notifications")
    }

    @Bean
    fun testDeclarables(testNotificationExchange: TopicExchange): Declarables {
        val testQueue = QueueBuilder.durable("test_queue").build()
        return Declarables(
            testQueue,
            BindingBuilder.bind(testQueue).to(testNotificationExchange).with("#"))
    }
}