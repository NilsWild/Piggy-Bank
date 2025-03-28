package de.rwth.swc.piggybank.transfergateway.service

import de.rwth.swc.piggybank.transfergateway.domain.Transfer
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

/**
 * Service for sending transfer events to RabbitMQ.
 */
@Service
class RabbitMQService(private val rabbitTemplate: RabbitTemplate) {
    private val logger = LoggerFactory.getLogger(RabbitMQService::class.java)

    companion object {
        const val EXCHANGE_NAME = "piggybank.transfers"
        const val ROUTING_KEY = "transfer.event"
    }

    /**
     * Sends a transfer event to RabbitMQ.
     *
     * @param transfer The transfer to send
     */
    fun sendTransferEvent(transfer: Transfer) {
        logger.info("Sending transfer event to RabbitMQ: {}", transfer)
        try {
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, transfer)
            logger.info("Transfer event sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send transfer event to RabbitMQ", e)
            throw e
        }
    }
}