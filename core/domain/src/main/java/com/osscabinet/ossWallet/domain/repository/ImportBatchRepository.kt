package com.osscabinet.ossWallet.domain.repository

/**
 * Records what a CSV import batch created (transactions tagged with the batch id, plus any
 * accounts/categories created fresh rather than matched), so it can be safely undone.
 */
interface ImportBatchRepository {
    suspend fun recordBatch(
        batchId: String,
        sourceApp: String,
        transactionCount: Int,
        createdAccountIds: List<String>,
        createdCategoryIds: List<Long>
    )

    /**
     * Deletes every transaction tagged with [batchId], then deletes any account/category from that
     * batch's created-id lists only if it now has zero remaining transactions — an account or
     * category the user has since used elsewhere is left alone. Returns false if the batch is
     * unknown (already undone, or never recorded).
     */
    suspend fun undoImport(batchId: String): Boolean
}
