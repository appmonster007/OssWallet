package com.osscabinet.ossWallet.data.repository

import com.osscabinet.ossWallet.data.local.SuggestionStateDataStore
import com.osscabinet.ossWallet.domain.repository.SuggestionStateRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class SuggestionStateRepositoryImpl @Inject constructor(
    private val store: SuggestionStateDataStore
) : SuggestionStateRepository {
    override fun dismissedKeys(profileId: Int) = store.dismissed(profileId)
    override fun seenKeys(profileId: Int) = store.seen(profileId)
    override suspend fun dismiss(profileId: Int, key: String) = store.dismiss(profileId, key)
    override suspend fun restoreDismissed(profileId: Int) = store.restoreDismissed(profileId)
    override suspend fun markSeen(profileId: Int, keys: Collection<String>) = store.markSeen(profileId, keys)
}
