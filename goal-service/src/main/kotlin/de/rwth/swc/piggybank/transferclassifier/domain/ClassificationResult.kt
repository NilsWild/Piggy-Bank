package de.rwth.swc.piggybank.transferclassifier.domain

import java.util.UUID

/**
 * Represents the result of classifying a transfer.
 * Contains the transfer ID and a list of classifications that matched the transfer.
 *
 * @property transferId The ID of the classified transfer
 * @property classifications The list of classifications that matched the transfer
 */
data class ClassificationResult(
    val transferId: UUID,
    val classifications: List<String>
){
    constructor(): this(
        transferId = UUID.randomUUID(),
        classifications = emptyList()
    )
}