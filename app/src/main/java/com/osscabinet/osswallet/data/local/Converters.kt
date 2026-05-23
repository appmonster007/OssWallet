package com.osscabinet.ossWallet.data.local

import androidx.room.TypeConverter
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.model.OccurrenceStatus
import com.osscabinet.ossWallet.domain.model.PaymentMethod
import com.osscabinet.ossWallet.domain.model.PlannedPaymentStatus
import com.osscabinet.ossWallet.domain.model.TransactionFilter
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.domain.model.timeline.TimelineRange
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

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus? {
        return value?.let { TransactionStatus.fromString(it) }
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? {
        return value?.let { PaymentMethod.fromString(it) }
    }

    @TypeConverter
    fun fromPlannedPaymentStatus(value: PlannedPaymentStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPlannedPaymentStatus(value: String?): PlannedPaymentStatus? {
        return value?.let { PlannedPaymentStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromOccurrenceStatus(value: OccurrenceStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toOccurrenceStatus(value: String?): OccurrenceStatus? {
        return value?.let { OccurrenceStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.value
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { TransactionType.fromValue(it) }
    }

    @TypeConverter
    fun fromTransactionTypeList(value: List<TransactionType>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toTransactionTypeList(value: String?): List<TransactionType>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromTransactionTypeSet(value: Set<TransactionType>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toTransactionTypeSet(value: String?): Set<TransactionType>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromLongSet(value: Set<Long>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toLongSet(value: String?): Set<Long>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromPaymentMethodSet(value: Set<PaymentMethod>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toPaymentMethodSet(value: String?): Set<PaymentMethod>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromTransactionStatusSet(value: Set<TransactionStatus>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toTransactionStatusSet(value: String?): Set<TransactionStatus>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromStringSet(value: Set<String>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringSet(value: String?): Set<String>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromCategoryNature(value: CategoryNature?): String? {
        return value?.value
    }

    @TypeConverter
    fun toCategoryNature(value: String?): CategoryNature? {
        return value?.let { CategoryNature.fromValue(it) }
    }
}
