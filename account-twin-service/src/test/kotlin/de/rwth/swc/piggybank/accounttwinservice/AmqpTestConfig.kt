package de.rwth.swc.piggybank.accounttwinservice

import de.rwth.swc.piggybank.accounttwinservice.config.RabbitMQTestConfig
import org.springframework.amqp.core.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class AmqpTestConfig {

    /**
     * Creates a test queue and binds it to the account exchange.
     * This allows the tests to receive messages sent to the exchange.
     *
     * @param accountExchange The account exchange
     * @return The declarables
     */
    @Bean
    fun testDeclarables(accountExchange: TopicExchange): Declarables {
        val testQueue = QueueBuilder.durable(RabbitMQTestConfig.TEST_QUEUE_NAME).build()
        return Declarables(
            testQueue,
            BindingBuilder.bind(testQueue).to(accountExchange).with("#"))
    }
}
