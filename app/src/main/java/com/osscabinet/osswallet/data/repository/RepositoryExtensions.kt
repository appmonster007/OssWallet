package com.osscabinet.ossWallet.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Extension functions for repositories to simplify mapping between entities and domain models.
 */

fun <T, R> Flow<List<T>>.mapToDomain(mapper: (T) -> R): Flow<List<R>> {
    return this.map { entities ->
        entities.map { mapper(it) }
    }
}

fun <T, R> List<T>.mapToDomain(mapper: (T) -> R): List<R> {
    return this.map { mapper(it) }
}
