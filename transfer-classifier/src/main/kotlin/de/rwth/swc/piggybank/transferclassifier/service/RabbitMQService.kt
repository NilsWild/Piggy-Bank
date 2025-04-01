package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transferclassifier.config.RabbitMQConfig
import de.rwth.swc.piggybank.transferclassifier.domain.ClassificationResult
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

/**
 * Service for sending classification results to RabbitMQ.
 */
@Service
class RabbitMQService(private val rabbitTemplate: RabbitTemplate) {
    private val logger = LoggerFactory.getLogger(RabbitMQService::class.java)

    /**
     * Sends a classification result to RabbitMQ.
     *
     * @param classificationResult The classification result to send
     */
    fun sendClassificationResult(classificationResult: ClassificationResult) {
        logger.info("Sending classification result to RabbitMQ: {}", classificationResult)
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CLASSIFICATION_EXCHANGE_NAME,
                RabbitMQConfig.CLASSIFICATION_ROUTING_KEY,
                classificationResult
            )
            logger.info("Classification result sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send classification result to RabbitMQ", e)
            throw e
        }
    }
}