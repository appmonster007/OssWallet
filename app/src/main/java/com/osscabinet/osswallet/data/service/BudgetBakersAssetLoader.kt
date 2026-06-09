package com.osscabinet.ossWallet.data.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class MappedCategory(
    val name: String,
    val parentName: String?
)

@Singleton
class BudgetBakersAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val envelopeMap: Map<Int, MappedCategory> by lazy { loadEnvelopeMap() }
    val paymentTypeMap: Map<String, String> by lazy { loadPaymentTypeMap() }

    private fun loadEnvelopeMap(): Map<Int, MappedCategory> {
        return try {
            val map = mutableMapOf<Int, MappedCategory>()
            context.assets.open("wallet-budgetbakers/envelope_map.csv").bufferedReader().forEachLine { line ->
                if (line.startsWith("envelope_id") || line.isBlank()) return@forEachLine
                val cols = line.split(",")
                val id = cols.getOrNull(0)?.trim()?.toIntOrNull() ?: return@forEachLine
                val ossName = cols.getOrNull(2)?.trim() ?: return@forEachLine
                val parentName = cols.getOrNull(3)?.trim()?.takeIf { it.isNotEmpty() }
                map[id] = MappedCategory(ossName, parentName)
            }
            map
        } catch (e: Exception) {
            Log.e("BudgetBakersAssets", "Failed to load envelope map", e)
            emptyMap()
        }
    }

    private fun loadPaymentTypeMap(): Map<String, String> {
        return try {
            val map = mutableMapOf<String, String>()
            context.assets.open("wallet-budgetbakers/payment_type_map.csv").bufferedReader().forEachLine { line ->
                if (line.startsWith("bb_") || line.isBlank()) return@forEachLine
                val cols = line.split(",")
                val bb = cols.getOrNull(0)?.trim() ?: return@forEachLine
                val oss = cols.getOrNull(1)?.trim() ?: return@forEachLine
                map[bb] = oss
            }
            map
        } catch (e: Exception) {
            Log.e("BudgetBakersAssets", "Failed to load payment type map", e)
            emptyMap()
        }
    }
}
