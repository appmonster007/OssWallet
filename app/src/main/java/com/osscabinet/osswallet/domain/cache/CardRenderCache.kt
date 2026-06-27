package com.osscabinet.ossWallet.domain.cache

import com.osscabinet.ossWallet.domain.card.CardRenderData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global cache for rendered dashboard cards.
 * Keyed by (profileId, currency, cardId) to allow for granular invalidation
 * and automatic cache-miss on currency changes.
 */
@Singleton
class CardRenderCache @Inject constructor() {
    // Key = (profileId, currency, cardId)
    private val cache = ConcurrentHashMap<Triple<Int, String, String>, CardRenderData>()

    operator fun get(profileId: Int, currency: String, cardId: String): CardRenderData? =
        cache[Triple(profileId, currency, cardId)]

    operator fun set(profileId: Int, currency: String, cardId: String, data: CardRenderData) {
        cache[Triple(profileId, currency, cardId)] = data
    }

    fun getProfileSnapshot(profileId: Int, currency: String): Map<String, CardRenderData> {
        return cache.entries
            .filter { (key, _) -> key.first == profileId && key.second == currency }
            .associate { (key, value) -> key.third to value }
    }

    fun invalidateProfile(profileId: Int) {
        cache.keys.removeAll { (pid, _, _) -> pid == profileId }
    }

    fun invalidateCard(profileId: Int, currency: String, cardId: String) {
        cache.remove(Triple(profileId, currency, cardId))
    }
}
