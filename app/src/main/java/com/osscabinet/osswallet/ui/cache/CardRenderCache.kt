package com.osscabinet.ossWallet.ui.cache

import com.osscabinet.ossWallet.domain.card.CardRenderData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRenderCache @Inject constructor() {
    private val cache = ConcurrentHashMap<Int, Map<String, CardRenderData>>()

    operator fun get(profileId: Int): Map<String, CardRenderData>? = cache[profileId]

    operator fun set(profileId: Int, data: Map<String, CardRenderData>) {
        cache[profileId] = data
    }
}
