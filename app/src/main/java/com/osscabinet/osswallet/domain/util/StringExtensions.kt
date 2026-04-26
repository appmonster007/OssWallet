package com.osscabinet.ossWallet.domain.util

/**
 * Extension functions for String and List<String> to handle comma-separated values.
 */

fun String.splitByComma(): List<String> {
    return if (this.isBlank()) {
        emptyList()
    } else {
        this.split(",").filter { it.isNotBlank() }
    }
}

fun <T> List<T>.joinByComma(): String {
    return this.joinToString(",")
}

fun <T> List<T>.joinByComma(transform: (T) -> CharSequence): String {
    return this.joinToString(",", transform = transform)
}
