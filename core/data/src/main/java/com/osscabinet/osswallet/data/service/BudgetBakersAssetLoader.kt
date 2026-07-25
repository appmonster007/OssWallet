package com.osscabinet.ossWallet.data.service

import android.content.Context
import android.util.Log
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.Serializable

@Serializable
data class MappedCategory(
    val name: String,
    val parentName: String? = null,
)

@Singleton
class BudgetBakersAssetLoader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val envelopeMap: Map<Int, MappedCategory> by lazy { loadEnvelopeMap() }
    val paymentTypeMap: Map<String, String> by lazy { loadPaymentTypeMap() }

    private fun loadEnvelopeMap(): Map<Int, MappedCategory> {
        return try {
            val settings = CsvParserSettings().apply {
                isHeaderExtractionEnabled = true
                skipEmptyLines = true
                ignoreLeadingWhitespaces = true
                ignoreTrailingWhitespaces = true
            }
            val parser = CsvParser(settings)

            context.assets.open("wallet-budgetbakers/envelope_map.csv").use { inputStream ->
                parser.iterateRecords(inputStream).associate { record ->
                    val id = record.getString("envelope_id").trim().toInt()
                    val ossName = record.getString("osswallet_category_name").trim()
                    val parentName = record.getString("parent_category_name")?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    id to MappedCategory(ossName, parentName)
                }
            }
        } catch (e: Exception) {
            Log.e("BudgetBakersAssets", "Failed to load envelope map", e)
            emptyMap()
        }
    }

    private fun loadPaymentTypeMap(): Map<String, String> {
        return try {
            val settings = CsvParserSettings().apply {
                isHeaderExtractionEnabled = true
                skipEmptyLines = true
                ignoreLeadingWhitespaces = true
                ignoreTrailingWhitespaces = true
            }
            val parser = CsvParser(settings)

            context.assets.open("wallet-budgetbakers/payment_type_map.csv").use { inputStream ->
                parser.iterateRecords(inputStream).associate { record ->
                    record.getString("bb_payment_type").trim() to
                            record.getString("osswallet_payment_method").trim()
                }
            }
        } catch (e: Exception) {
            Log.e("BudgetBakersAssets", "Failed to load payment type map", e)
            emptyMap()
        }
    }
}
