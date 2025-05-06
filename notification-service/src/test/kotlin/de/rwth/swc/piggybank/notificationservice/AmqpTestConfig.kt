package de.rwth.swc.piggybank.notificationservice

import de.rwth.swc.piggybank.notificationservice.service.RabbitMQService
import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {

    @Bean
    fun testDeclarables(notificationExchange: TopicExchange): Declarables {
        val testQueue = QueueBuilder.durable("test_queue").build()
        return Declarables(
            testQueue,
            BindingBuilder.bind(testQueue).to(notificationExchange).with(RabbitMQService.NOTIFICATION_ROUTING_KEY))
    }
}