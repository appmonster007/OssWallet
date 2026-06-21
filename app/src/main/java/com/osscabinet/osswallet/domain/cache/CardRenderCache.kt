package com.osscabinet.ossWallet.domain.cache

import com.osscabinet.ossWallet.domain.card.CardRenderData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRenderCache @Inject constructor() {
    // ConcurrentHashMap is intentional: reads (operator get) are lock-free and happen outside
    // the ViewModel's Mutex. Writes (operator set) always occur inside mutex.withLock{} in the
    // calling ViewModel, so the full read-filter-write sequence is externally serialized.
    // A plain HashMap would require synchronization for the unsynchronized reads.
    private val cache = ConcurrentHashMap<Int, Map<String, CardRenderData>>()

    operator fun get(profileId: Int): Map<String, CardRenderData>? = cache[profileId]

    operator fun set(profileId: Int, data: Map<String, CardRenderData>) {
        cache[profileId] = data
    }
}
