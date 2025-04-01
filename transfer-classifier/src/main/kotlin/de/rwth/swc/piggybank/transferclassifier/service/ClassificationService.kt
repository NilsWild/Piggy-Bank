package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transferclassifier.domain.ClassificationResult
import de.rwth.swc.piggybank.transferclassifier.domain.Transfer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for classifying transfers.
 * Processes transfers through all available classifiers and returns the classification results.
 */
@Service
class ClassificationService(private val classifiers: List<TransferClassifier>) {
    private val logger = LoggerFactory.getLogger(ClassificationService::class.java)

    /**
     * Classifies a transfer using all available classifiers.
     *
     * @param transfer The transfer to classify
     * @return The classification result containing the transfer ID and the list of matching classifications
     */
    fun classifyTransfer(transfer: Transfer): ClassificationResult {
        logger.info("Classifying transfer with ID: {}", transfer.id)
        
        val matchingClassifications = classifiers
            .filter { classifier -> classifier.classify(transfer) }
            .map { classifier -> classifier.name }
        
        logger.info("Transfer {} classified as: {}", transfer.id, matchingClassifications)
        
        return ClassificationResult(
            transferId = transfer.id,
            classifications = matchingClassifications
        )
    }
}