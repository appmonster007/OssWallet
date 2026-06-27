package com.osscabinet.ossWallet.domain.cache

import com.osscabinet.ossWallet.domain.card.CardRenderData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global cache for rendered dashboard cards.
 * Keyed by (profileId, currency, cardId) for granular invalidation.
 * Maintains a secondary index keyed by (profileId, currency) for O(1) profile snapshots.
 */
@Singleton
class CardRenderCache @Inject constructor() {
    private val cache = ConcurrentHashMap<Triple<Int, String, String>, CardRenderData>()

    // Secondary index: (profileId, currency) → map of cardId → data
    private val profileIndex = ConcurrentHashMap<Pair<Int, String>, ConcurrentHashMap<String, CardRenderData>>()

    operator fun get(profileId: Int, currency: String, cardId: String): CardRenderData? =
        cache[Triple(profileId, currency, cardId)]

    operator fun set(profileId: Int, currency: String, cardId: String, data: CardRenderData) {
        cache[Triple(profileId, currency, cardId)] = data
        profileIndex.getOrPut(Pair(profileId, currency)) { ConcurrentHashMap() }[cardId] = data
    }

    fun getProfileSnapshot(profileId: Int, currency: String): Map<String, CardRenderData> =
        profileIndex[Pair(profileId, currency)]?.toMap() ?: emptyMap()

    fun invalidateProfile(profileId: Int) {
        val keysToRemove = cache.keys.filter { it.first == profileId }
        keysToRemove.forEach { cache.remove(it) }
        profileIndex.keys.filter { it.first == profileId }.forEach { profileIndex.remove(it) }
    }

    fun invalidateCard(profileId: Int, currency: String, cardId: String) {
        cache.remove(Triple(profileId, currency, cardId))
        profileIndex[Pair(profileId, currency)]?.remove(cardId)
    }
}
