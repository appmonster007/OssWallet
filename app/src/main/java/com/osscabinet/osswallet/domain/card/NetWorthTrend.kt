package com.osscabinet.ossWallet.domain.card

import com.osscabinet.ossWallet.domain.service.currency.CurrencyService
import com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot
import com.osscabinet.ossWallet.domain.service.timeline.TimelineService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds the Net Worth trend line from transactions.
 *
 * There is no stored balance history, so the line is the opening balance (net flow before the
 * period) plus the running net flow of each bucket. Every point is converted with the single set of
 * current exchange rates, so for multi-currency data it shows today's value of the holdings, not
 * what they were worth in base currency at the time.
 */
object NetWorthTrend {

    private val dayKeyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * @param buckets (start, end, label) triples in ascending order, as from [CardComputerUtils.generateBuckets]
     * @param openingByCurrency net flow per currency from all transactions before the first bucket
     * @param dailyNetByCurrency net flow per currency keyed by "yyyy-MM-dd"
     * @return one point per bucket, or empty when there are fewer than two buckets
     */
    fun build(
        buckets: List<Triple<Long, Long, String>>,
        openingByCurrency: Map<String, BigDecimal>,
        dailyNetByCurrency: Map<String, Map<String, BigDecimal>>,
        targetCurrency: String,
        exchangeRates: ExchangeRateSnapshot
    ): List<NetWorthTrendPoint> {
        if (buckets.size < 2) return emptyList()

        val zone = TimelineService.systemZoneId
        val bucketStarts = buckets.map { it.first }
        val deltaPerBucket = Array(buckets.size) { mutableMapOf<String, BigDecimal>() }

        dailyNetByCurrency.forEach { (dayKey, byCurrency) ->
            val dayStart = try {
                LocalDate.parse(dayKey, dayKeyFormatter).atStartOfDay(zone).toInstant().toEpochMilli()
            } catch (_: Exception) {
                return@forEach
            }
            val idx = bucketStarts.indexOfLast { it <= dayStart }
            if (idx < 0) return@forEach
            byCurrency.forEach { (currency, amount) ->
                deltaPerBucket[idx].merge(currency, amount, BigDecimal::add)
            }
        }

        val running = openingByCurrency.toMutableMap()
        return buckets.mapIndexed { i, (start, _, label) ->
            deltaPerBucket[i].forEach { (currency, amount) -> running.merge(currency, amount, BigDecimal::add) }
            NetWorthTrendPoint(
                label = label,
                epochMillis = start,
                value = CurrencyService.calculateTotalInCurrency(
                    currencyTotals = running,
                    targetCurrency = targetCurrency,
                    exchangeRates = exchangeRates
                )
            )
        }
    }
}
