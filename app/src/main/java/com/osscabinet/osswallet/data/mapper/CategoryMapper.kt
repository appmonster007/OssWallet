package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.CategoryEntity
import com.osscabinet.ossWallet.domain.model.Category

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isDefault = isDefault,
        parentId = parentId,
        nature = nature
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isDefault = isDefault,
        parentId = parentId,
        nature = nature
    )
}
