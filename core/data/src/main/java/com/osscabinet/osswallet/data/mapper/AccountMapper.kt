package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.AccountEntity
import com.osscabinet.ossWallet.data.local.entity.CreditCardDetailsEntity
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.CreditCardDetails
import com.osscabinet.ossWallet.domain.util.joinByComma
import com.osscabinet.ossWallet.domain.util.splitByComma

/**
 * Mappers for Account data.
 * Centralizes transformation between Room entities and Domain models.
 */
fun AccountEntity.toDomain(creditDetails: CreditCardDetails? = null) = Account(
    id = id,
    name = name,
    currencyCode = currencyCode,
    structureType = AccountStructureType.fromString(structureType),
    colorHex = colorHex,
    icon = icon,
    allowedCurrencies = allowedCurrencies.splitByComma(),
    isArchived = isArchived,
    displayOrder = displayOrder,
    creditDetails = creditDetails
)

fun Account.toEntity(profileId: Int) = AccountEntity(
    id = id,
    name = name,
    currencyCode = currencyCode,
    structureType = structureType.displayName,
    colorHex = colorHex,
    icon = icon,
    allowedCurrencies = allowedCurrencies.joinByComma(),
    profileId = profileId,
    isArchived = isArchived,
    displayOrder = displayOrder
)

fun CreditCardDetailsEntity.toDomain() = CreditCardDetails(
    accountId = accountId,
    creditLimit = creditLimit,
    statementClosingDay = statementClosingDay,
    paymentDueDay = paymentDueDay,
    interestRateApr = interestRateApr,
    minimumPaymentAmount = minimumPaymentAmount
)

fun CreditCardDetails.toEntity() = CreditCardDetailsEntity(
    accountId = accountId,
    creditLimit = creditLimit,
    statementClosingDay = statementClosingDay,
    paymentDueDay = paymentDueDay,
    interestRateApr = interestRateApr,
    minimumPaymentAmount = minimumPaymentAmount
)
