package com.osscabinet.gleemanager.data.local

import com.osscabinet.gleemanager.data.local.entity.*
import kotlinx.serialization.Serializable

@Serializable
data class GleeBackup(
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
    val balance: Double,
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
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val currency: String,
    val isIncome: Boolean,
    val accountId: String,
    val categoryId: Long?,
    val tags: String,
    val paymentMethod: String = "Cash",
    val profileId: Int
)

@Serializable
data class TemplateBackup(
    val id: Long,
    val name: String,
    val category: String,
    val amount: Double,
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
