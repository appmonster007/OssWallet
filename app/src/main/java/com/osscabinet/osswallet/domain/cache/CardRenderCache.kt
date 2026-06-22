package com.osscabinet.ossWallet.domain.cache

import com.osscabinet.ossWallet.domain.card.CardRenderData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRenderCache @Inject constructor() {
    // Key = (profileId, currency) so a currency change automatically misses the cache,
    // preventing stale cards computed with the old exchange rates from being shown.
    // ConcurrentHashMap: lock-free reads, externally serialized writes via ViewModel Mutex.
    private val cache = ConcurrentHashMap<Pair<Int, String>, Map<String, CardRenderData>>()

    operator fun get(profileId: Int, currency: String): Map<String, CardRenderData>? =
        cache[profileId to currency]

    operator fun set(profileId: Int, currency: String, data: Map<String, CardRenderData>) {
        cache[profileId to currency] = data
    }

    fun invalidateProfile(profileId: Int) {
        cache.keys.removeAll { (pid, _) -> pid == profileId }
    }
}
