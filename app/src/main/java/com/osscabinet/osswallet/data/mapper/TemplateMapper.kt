package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.TemplateEntity
import com.osscabinet.ossWallet.domain.model.Template

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
        gpsLocation = gpsLocation,
        warrantyExpiration = warrantyExpiration,
        transactionStatus = transactionStatus,
        paymentMethod = paymentMethod,
        currency = currency,
        tags = tags,
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
        gpsLocation = gpsLocation,
        warrantyExpiration = warrantyExpiration,
        transactionStatus = transactionStatus,
        paymentMethod = paymentMethod,
        currency = currency,
        tags = tags,
        isStarred = isStarred,
        displayOrder = displayOrder,
        profileId = profileId
    )
}
