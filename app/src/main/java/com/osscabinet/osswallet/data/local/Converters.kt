package com.osscabinet.ossWallet.data.local

import androidx.room.TypeConverter
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
}
