package com.osscabinet.ossWallet.domain.service.mlkit

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.entityextraction.DateTimeEntity
import com.google.mlkit.nl.entityextraction.EntityAnnotation
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.google.mlkit.nl.entityextraction.MoneyEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.dto.TransactionPrefillDTO
import kotlinx.coroutines.suspendCancellableCoroutine
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * `full` flavor implementation: asks Google ML Kit Entity Extraction for the amount, currency and
 * date, and lets [TransactionTextHeuristics] supply everything ML Kit did not find (and the payee,
 * category, account and intent, which it never provides).
 */
@Singleton
class MlKitTransactionDecoder @Inject constructor() {

    /** Set when the last ML Kit call failed and the decoder fell back to regex. Null means ML Kit ran. */
    internal var lastMlKitError: Throwable? = null
        private set

    /** Number of entities ML Kit returned for the last decode. Zero means the regex fallback did the work. */
    internal var lastMlKitEntityCount: Int = 0
        private set

    suspend fun decodeText(
        rawText: String,
        accounts: List<Account>,
        categories: List<Category>
    ): TransactionPrefillDTO {
        val extracted = if (rawText.isBlank()) {
            TransactionTextHeuristics.Extracted()
        } else {
            extractedFrom(extractMlKitEntities(rawText), rawText)
        }
        return TransactionTextHeuristics.buildPrefill(rawText, accounts, categories, extracted)
    }

    // The first money entity and the first date entity ML Kit found; the currency is read from the
    // text around the money entity.
    private fun extractedFrom(annotations: List<EntityAnnotation>, rawText: String): TransactionTextHeuristics.Extracted {
        var amount: BigDecimal? = null
        var currency: String? = null
        var timestamp: Long? = null

        for (annotation in annotations) {
            for (entity in annotation.entities) {
                when {
                    entity is MoneyEntity && amount == null -> {
                        val total = BigDecimal.valueOf(entity.integerPart.toLong())
                            .add(BigDecimal.valueOf(entity.fractionalPart.toLong(), 2))
                        if (total > BigDecimal.ZERO) {
                            amount = total
                            val span = rawText.substring(annotation.start, minOf(annotation.end, rawText.length))
                            currency = TransactionTextHeuristics.parseCurrency(span, rawText)
                        }
                    }
                    entity is DateTimeEntity && timestamp == null -> timestamp = entity.timestampMillis
                }
            }
        }
        return TransactionTextHeuristics.Extracted(amount, currency, timestamp)
    }

    private suspend fun extractMlKitEntities(text: String): List<EntityAnnotation> {
        lastMlKitError = null
        lastMlKitEntityCount = 0
        return try {
            val entityExtractor = EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )
            if (!entityExtractor.isModelDownloaded().awaitTask()) {
                entityExtractor.downloadModelIfNeeded().awaitTask()
            }
            val annotations = entityExtractor.annotate(EntityExtractionParams.Builder(text).build()).awaitTask()
            lastMlKitEntityCount = annotations.sumOf { it.entities.size }
            entityExtractor.close()
            annotations
        } catch (e: Exception) {
            lastMlKitError = e
            emptyList()
        } catch (e: LinkageError) {
            // ML Kit bytecode failed to load/verify (e.g. JVM unit tests); fall back to regex parsing.
            lastMlKitError = e
            emptyList()
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }
