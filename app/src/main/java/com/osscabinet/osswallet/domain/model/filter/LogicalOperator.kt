package com.osscabinet.ossWallet.domain.model.filter

import kotlinx.serialization.Serializable

/**
 * Logical operators for combining multiple filters.
 */
@Serializable
enum class LogicalOperator {
    AND,
    OR
}
