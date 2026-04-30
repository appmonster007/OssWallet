package com.osscabinet.ossWallet.data.local

import androidx.room.TypeConverter
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun fromTimelineRange(value: TimelineRange?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toTimelineRange(value: String?): TimelineRange? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromTransactionFilter(value: TransactionFilter?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toTransactionFilter(value: String?): TransactionFilter? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromFloatSet(value: Set<Float>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toFloatSet(value: String?): Set<Float>? {
        return value?.let { Json.decodeFromString(it) }
    }
}
