package com.osscabinet.ossWallet.domain.util

/**
 * Extension functions for Enum serialization/deserialization.
 * Provides consistent patterns for converting between enums and strings.
 */

/**
 * Safely converts a string to an enum value, ignoring case.
 * Returns null if the string doesn't match any enum value.
 */
inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? {
    return try {
        java.lang.Enum.valueOf(T::class.java, this)
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * Converts a string to an enum value, ignoring case.
 * Throws IllegalArgumentException if the string doesn't match any enum value.
 */
inline fun <reified T : Enum<T>> String.toEnum(): T {
    return java.lang.Enum.valueOf(T::class.java, this)
}

/**
 * Converts a list of strings to a list of enum values.
 * Skips invalid strings rather than throwing exceptions.
 */
inline fun <reified T : Enum<T>> List<String>.toEnumList(): List<T> {
    return mapNotNull { it.toEnumOrNull<T>() }
}

/**
 * Converts a list of enum values to a list of their names.
 */
fun <T : Enum<T>> List<T>.toNameList(): List<String> {
    return map { it.name }
}
