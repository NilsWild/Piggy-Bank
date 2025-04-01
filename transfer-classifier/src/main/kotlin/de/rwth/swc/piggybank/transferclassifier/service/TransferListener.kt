package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transferclassifier.config.RabbitMQConfig
import de.rwth.swc.piggybank.transferclassifier.domain.Transfer
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * Listener for transfer events from RabbitMQ.
 */
@Component
class TransferListener(
    private val classificationService: ClassificationService,
    private val rabbitMQService: RabbitMQService
) {
    private val logger = LoggerFactory.getLogger(TransferListener::class.java)

    /**
     * Listens for transfer events from RabbitMQ, classifies them, and sends the classification results.
     *
     * @param transfer The transfer received from RabbitMQ
     */
    @RabbitListener(queues = [RabbitMQConfig.TRANSFER_QUEUE_NAME])
    fun receiveTransfer(transfer: Transfer) {
        logger.info("Received transfer with ID: {}", transfer.id)
        try {
            // Classify the transfer
            val classificationResult = classificationService.classifyTransfer(transfer)
            
            // Send the classification result to RabbitMQ
            rabbitMQService.sendClassificationResult(classificationResult)
            
            logger.info("Transfer processed successfully")
        } catch (e: Exception) {
            logger.error("Failed to process transfer", e)
        }
    }
}