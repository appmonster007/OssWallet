package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.util.splitByComma
import com.osscabinet.ossWallet.domain.util.joinByComma

/**
 * Mappers for Account data.
 * Centralizes transformation between Room entities and Domain models.
 */
fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    currencyCode = currencyCode,
    structureType = structureType,
    colorHex = colorHex,
    icon = icon,
    allowedCurrencies = allowedCurrencies.splitByComma(),
    isArchived = isArchived,
    displayOrder = displayOrder
)

fun Account.toEntity(profileId: Int) = AccountEntity(
    id = id,
    name = name,
    currencyCode = currencyCode,
    structureType = structureType,
    colorHex = colorHex,
    icon = icon,
    allowedCurrencies = allowedCurrencies.joinByComma(),
    profileId = profileId,
    isArchived = isArchived,
    displayOrder = displayOrder
)
