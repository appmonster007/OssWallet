package com.osscabinet.ossWallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.osscabinet.ossWallet.domain.model.CategoryNature

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val isDefault: Boolean = false,
    val parentId: Long? = null,
    val nature: CategoryNature = CategoryNature.WANT
)
