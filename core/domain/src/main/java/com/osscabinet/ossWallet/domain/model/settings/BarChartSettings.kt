package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

/** How a bar chart card lays out its buckets. */
@Serializable
enum class BarChartViewMode(val displayName: String) {
    /** Every bucket is squeezed into the card width (granularity is picked from the range). */
    FIXED("Fixed scale"),

    /** A fixed number of buckets per screen; the rest scrolls horizontally. */
    SCROLLABLE("Scrollable")
}

/** Bucket size for a scrollable bar chart. [AUTO] picks day / week / month from the range span. */
@Serializable
enum class BarChartGranularity(val displayName: String) {
    AUTO("Auto"),
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    QUARTER("Quarter"),
    YEAR("Year")
}
