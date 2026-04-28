package com.osscabinet.ossWallet.domain.model

data class Profile(
    val id: Int = 0,
    val name: String,
    val baseCurrency: String,
    val type: ProfileType = ProfileType.PERSONAL
)

enum class ProfileType {
    PERSONAL,
    BUSINESS
}
