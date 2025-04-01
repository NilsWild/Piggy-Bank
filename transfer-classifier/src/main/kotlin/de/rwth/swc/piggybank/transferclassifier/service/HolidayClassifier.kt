package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transferclassifier.domain.Transfer
import org.springframework.stereotype.Component

/**
 * Classifier for holiday transfers.
 * Checks if the transfer purpose contains holiday-related words like hotel or rental.
 */
@Component
class HolidayClassifier : TransferClassifier {
    override val name: String = "Holiday"

    private val holidayKeywords = listOf("hotel", "rental", "vacation", "resort", "beach", "travel", "holiday")

    /**
     * Classifies a transfer as a holiday transfer if its purpose contains any of the holiday-related keywords.
     *
     * @param transfer The transfer to classify
     * @return true if the transfer purpose contains any of the holiday-related keywords, false otherwise
     */
    override fun classify(transfer: Transfer): Boolean {
        val purpose = transfer.purpose.lowercase()
        return holidayKeywords.any { keyword -> purpose.contains(keyword.lowercase()) }
    }
}
