package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transferclassifier.domain.Transfer

/**
 * Interface for transfer classifiers.
 * Implementations of this interface classify transfers based on specific criteria.
 */
interface TransferClassifier {
    /**
     * The name of the classifier.
     */
    val name: String

    /**
     * Classifies a transfer.
     *
     * @param transfer The transfer to classify
     * @return true if the transfer matches the classification criteria, false otherwise
     */
    fun classify(transfer: Transfer): Boolean
}