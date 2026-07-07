package com.osscabinet.ossWallet.data.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.Serializable
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

@Serializable
data class MappedCategory(
    val name: String,
    val parentName: String? = null
)

@Singleton
class BudgetBakersAssetLoader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val envelopeMap: Map<Int, MappedCategory> by lazy { loadEnvelopeMap() }
    val paymentTypeMap: Map<String, String> by lazy { loadPaymentTypeMap() }

    private fun loadEnvelopeMap(): Map<Int, MappedCategory> {
        return try {
            val format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()

            context.assets.open("wallet-budgetbakers/envelope_map.csv").bufferedReader()
                .use { reader ->
                    CSVParser.parse(reader, format).associate { record ->
                        val id = record.get("envelope_id").trim().toInt()
                        val ossName = record.get("osswallet_category_name").trim()
                        val parentName =
                            record.get("parent_category_name").trim().takeIf { it.isNotEmpty() }
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
            val format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()

            context.assets.open("wallet-budgetbakers/payment_type_map.csv").bufferedReader()
                .use { reader ->
                    CSVParser.parse(reader, format).associate { record ->
                        record.get("bb_payment_type")
                            .trim() to record.get("osswallet_payment_method").trim()
                    }
                }
        } catch (e: Exception) {
            Log.e("BudgetBakersAssets", "Failed to load payment type map", e)
            emptyMap()
        }
    }
}
