package com.osscabinet.ossWallet.domain.model.filter

import kotlinx.serialization.Serializable

/**
 * Operators for atomic filter conditions.
 * Defines how a filter criterion matches against data.
 */
@Serializable
enum class FilterOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    IN,
    NOT_IN,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    BETWEEN,
    REGEX
}
