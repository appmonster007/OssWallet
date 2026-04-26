package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.TransactionEntity
import com.osscabinet.ossWallet.domain.model.Transaction
import com.osscabinet.ossWallet.domain.model.TransactionStatus
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma

/**
 * Mappers for Transaction data.
 * Centralizes transformation between Room entities and Domain models.
 */
fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        title = title,
        amount = amount,
        category = category,
        timestamp = timestamp,
        currency = currency,
        transactionType = transactionType,
        accountId = accountId,
        categoryId = categoryId,
        tags = tags.splitByComma(),
        paymentMethod = paymentMethod,
        payee = payee,
        note = note,
        recurring = recurring,
        latitude = latitude,
        longitude = longitude,
        warrantyExpiration = warrantyExpiration,
        status = TransactionStatus.fromString(status)
    )
}

fun Transaction.toEntity(profileId: Int): TransactionEntity {
    return TransactionEntity(
        id = id,
        title = title,
        amount = amount,
        category = category,
        timestamp = timestamp,
        currency = currency,
        transactionType = transactionType,
        accountId = accountId,
        categoryId = categoryId,
        tags = tags.joinByComma(),
        profileId = profileId,
        paymentMethod = paymentMethod,
        payee = payee,
        note = note,
        recurring = recurring,
        latitude = latitude,
        longitude = longitude,
        warrantyExpiration = warrantyExpiration,
        status = status.name
    )
}
