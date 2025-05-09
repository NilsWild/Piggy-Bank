package de.rwth.swc.piggybank.transferclassifier.service

import de.rwth.swc.piggybank.transfergateway.domain.Transfer
import org.springframework.stereotype.Component

/**
 * Classifier for grocery transfers.
 * Checks if the transfer purpose contains grocery store names like Aldi, Lidl, or Edeka.
 */
@Component
class GroceryClassifier : TransferClassifier {
    override val name: String = "Grocery"

    private val groceryStores = listOf("Aldi", "Lidl", "Edeka")

    /**
     * Classifies a transfer as a grocery transfer if its purpose contains any of the grocery store names.
     *
     * @param transfer The transfer to classify
     * @return true if the transfer purpose contains any of the grocery store names, false otherwise
     */
    override fun classify(transfer: Transfer): Boolean {
        val purpose = transfer.purpose.lowercase()
        return groceryStores.any { store -> purpose.contains(store.lowercase()) }
    }
}