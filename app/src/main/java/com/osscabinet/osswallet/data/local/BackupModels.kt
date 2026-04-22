package com.osscabinet.ossWallet.data.local

import com.osscabinet.ossWallet.data.local.entity.*
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}

@Serializable
data class OssWalletBackup(
    val profiles: List<ProfileBackup>,
    val accounts: List<AccountBackup>,
    val transactions: List<TransactionBackup>,
    val categories: List<CategoryBackup>,
    val tags: List<TagBackup>,
    val templates: List<TemplateBackup> = emptyList()
)

@Serializable
data class ProfileBackup(val id: Int, val name: String, val type: String)

@Serializable
data class AccountBackup(
    val id: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal,
    val currencyCode: String,
    val structureType: String,
    val colorHex: String,
    val icon: String,
    val allowedCurrencies: String,
    val profileId: Int
)

@Serializable
data class TransactionBackup(
    val id: Long,
    val title: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val category: String,
    val timestamp: Long,
    val currency: String,
    val transactionType: String,
    val accountId: String,
    val categoryId: Long?,
    val tags: String,
    val paymentMethod: String = "Cash",
    val payee: String = "",
    val note: String = "",
    val recurring: String? = null,
    val linkedTransactionId: Long? = null,
    val profileId: Int,
    val status: String = "cleared"
)

@Serializable
data class TemplateBackup(
    val id: Long,
    val name: String,
    val category: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val transactionType: Int,
    val accountId: String?,
    val note: String?,
    val payee: String?,
    val location: String?,
    val paymentMethod: String?,
    val currency: String?,
    val tags: String,
    val isStarred: Boolean,
    val displayOrder: Int
)

@Serializable
data class CategoryBackup(val id: Long, val name: String, val iconName: String, val colorHex: Int, val isDefault: Boolean)

@Serializable
data class TagBackup(val id: Long, val name: String, val colorHex: Int)
