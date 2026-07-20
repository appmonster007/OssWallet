package com.osscabinet.ossWallet.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CategoryNature(val value: String) {
    MUST("must"),
    NEED("need"),
    WANT("want");

    companion object {
        fun fromValue(value: String): CategoryNature {
            return entries.find { it.value.lowercase() == value.lowercase() } ?: NEED
        }
    }
}
