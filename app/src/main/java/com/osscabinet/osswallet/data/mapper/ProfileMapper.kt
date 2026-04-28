package com.osscabinet.ossWallet.data.mapper

import com.osscabinet.ossWallet.data.local.entity.ProfileEntity
import com.osscabinet.ossWallet.domain.model.Profile
import com.osscabinet.ossWallet.domain.model.ProfileType
import com.osscabinet.ossWallet.domain.util.toEnum

fun ProfileEntity.toDomain() = Profile(
    id = id,
    name = name,
    baseCurrency = baseCurrency,
    type = type.toEnum<ProfileType>()
)

fun Profile.toEntity() = ProfileEntity(
    id = id,
    name = name,
    baseCurrency = baseCurrency,
    type = type.name
)
