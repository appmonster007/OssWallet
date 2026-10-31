package com.osscabinet.ossWallet.domain.repository

import kotlinx.coroutines.flow.Flow

/** What the user has done with recurring-payment suggestions, per profile. Keys are `DiscoveredPattern.key`. */
interface SuggestionStateRepository {
    /** Suggestions the user said no to; they stay hidden. */
    fun dismissedKeys(profileId: Int): Flow<Set<String>>

    /** Suggestions the user has already looked at, so the dashboard only nudges about new ones. */
    fun seenKeys(profileId: Int): Flow<Set<String>>

    suspend fun dismiss(profileId: Int, key: String)
    suspend fun restoreDismissed(profileId: Int)
    suspend fun markSeen(profileId: Int, keys: Collection<String>)
}
