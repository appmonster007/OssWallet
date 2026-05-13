package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionStatus

fun TemplateEntity.toDomain(): Template {
    return Template(
        id = id,
        name = name,
        categoryId = categoryId,
        amount = amount,
        transactionType = transactionType,
        icon = icon,
        colorHex = colorHex,
        accountId = accountId,
        targetAccountId = targetAccountId,
        targetCurrency = targetCurrency,
        transferRate = transferRate,
        note = note,
        payee = payee,
        location = location,
        latitude = latitude,
        longitude = longitude,
        warrantyExpiration = warrantyExpiration,
        transactionStatus = try { TransactionStatus.valueOf(transactionStatus) } catch (e: Exception) { TransactionStatus.CLEARED },
        paymentMethod = paymentMethod,
        currency = currency,
        tags = if (tags.isEmpty()) emptyList() else tags.split(","),
        isStarred = isStarred,
        displayOrder = displayOrder,
        profileId = profileId
    )
}

fun Template.toEntity(): TemplateEntity {
    return TemplateEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        amount = amount,
        transactionType = transactionType,
        icon = icon,
        colorHex = colorHex,
        accountId = accountId,
        targetAccountId = targetAccountId,
        targetCurrency = targetCurrency,
        transferRate = transferRate,
        note = note,
        payee = payee,
        location = location,
        latitude = latitude,
        longitude = longitude,
        warrantyExpiration = warrantyExpiration,
        transactionStatus = transactionStatus.name,
        paymentMethod = paymentMethod,
        currency = currency,
        tags = tags.joinToString(","),
        isStarred = isStarred,
        displayOrder = displayOrder,
        profileId = profileId
    )
}
