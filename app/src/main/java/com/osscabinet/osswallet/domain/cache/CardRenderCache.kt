package com.osscabinet.ossWallet.domain.cache

import com.osscabinet.ossWallet.data.local.dao.CardCacheDao
import com.osscabinet.ossWallet.data.local.entity.CardCacheEntity
import com.osscabinet.ossWallet.domain.card.CardRenderData
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.osscabinet.ossWallet.BuildConfig

/**
 * Global cache for rendered dashboard cards.
 * Keyed by (profileId, currency, cardId) for granular invalidation.
 * Supports persistent storage in Room with version tracking via transactions' lastUpdated.
 */
@Singleton
class CardRenderCache @Inject constructor(
    private val cardCacheDao: CardCacheDao,
    private val json: Json
) {
    // Internal scope for background DB loads — supervisor so one failure doesn't cancel others.
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cache = ConcurrentHashMap<Triple<Int, String, String>, CardRenderData>()
    private val versionCache = ConcurrentHashMap<Triple<Int, String, String>, Long>()

    // Secondary index: (profileId, currency) → map of cardId → data
    private val profileIndex = ConcurrentHashMap<Pair<Int, String>, ConcurrentHashMap<String, CardRenderData>>()

    // Tracks in-flight or completed profile loads so callers can await without double-loading.
    private val pendingLoads = ConcurrentHashMap<Pair<Int, String>, Deferred<Boolean>>()

    operator fun get(profileId: Int, currency: String, cardId: String): CardRenderData? =
        cache[Triple(profileId, currency, cardId)]

    fun getVersion(profileId: Int, currency: String, cardId: String): Long? =
        versionCache[Triple(profileId, currency, cardId)]

    /**
     * Synchronously sets the in-memory cache and asynchronously updates the persistent Room cache.
     */
    fun set(profileId: Int, currency: String, cardId: String, data: CardRenderData, version: Long) {
        cache[Triple(profileId, currency, cardId)] = data
        versionCache[Triple(profileId, currency, cardId)] = version
        
        profileIndex.compute(Pair(profileId, currency)) { _, existing ->
            (existing ?: ConcurrentHashMap()).also { it[cardId] = data }
        }

        // Persistent update (Fire and forget, but ideally supervised)
        // In a real app, this should be launched in a dedicated scope
    }

    /**
     * Version-aware setter that persists to DB.
     */
    suspend fun persist(profileId: Int, currency: String, cardId: String, data: CardRenderData, version: Long) {
        withContext(Dispatchers.IO) {
            set(profileId, currency, cardId, data, version)
            val jsonStr = json.encodeToString(data)
            cardCacheDao.insert(
                CardCacheEntity(
                    profileId = profileId,
                    currency = currency,
                    cardId = cardId,
                    dataJson = jsonStr,
                    version = version
                )
            )
        }
    }

    /**
     * Loads the entire profile cache from DB into memory.
     * Returns true if any data was loaded.
     */
    suspend fun loadProfile(profileId: Int, currency: String): Boolean = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (BuildConfig.DEBUG) Log.d("OssPerf", "[Cache] loadProfile START profile=$profileId currency=$currency")
        val entities = cardCacheDao.getCacheForProfile(profileId, currency)
        if (entities.isEmpty()) {
            if (BuildConfig.DEBUG) Log.d("OssPerf", "[Cache] loadProfile END — 0 cards (empty cache) in ${System.currentTimeMillis() - start}ms")
            return@withContext false
        }

        var loaded = 0
        entities.forEach { entity ->
            try {
                val data = json.decodeFromString<CardRenderData>(entity.dataJson)
                cache[Triple(profileId, currency, entity.cardId)] = data
                versionCache[Triple(profileId, currency, entity.cardId)] = entity.version
                profileIndex.compute(Pair(profileId, currency)) { _, existing ->
                    (existing ?: ConcurrentHashMap()).also { it[entity.cardId] = data }
                }
                loaded++
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w("OssPerf", "[Cache] deserialize failed for cardId=${entity.cardId}: ${e.message}")
                // Prune the stale row so it doesn't fail on every subsequent boot.
                cardCacheDao.deleteByCardId(profileId, currency, entity.cardId)
            }
        }
        if (BuildConfig.DEBUG) Log.d("OssPerf", "[Cache] loadProfile END — $loaded/${entities.size} cards loaded in ${System.currentTimeMillis() - start}ms")
        true
    }

    fun getProfileSnapshot(profileId: Int, currency: String): Map<String, CardRenderData> =
        profileIndex[Pair(profileId, currency)]?.toMap() ?: emptyMap()

    /**
     * Starts a background DB load for this profile+currency if one hasn't been started yet.
     * Idempotent — returns the same Deferred if called multiple times for the same key.
     * Call this as early as possible (e.g. ViewModel init) so the load races ingredient queries.
     */
    fun kickOffProfileLoad(profileId: Int, currency: String): Deferred<Boolean> =
        pendingLoads.computeIfAbsent(Pair(profileId, currency)) {
            cacheScope.async { loadProfile(profileId, currency) }
        }

    /**
     * Ensures the profile cache is in memory before returning.
     * Short-circuits immediately if already loaded. Otherwise awaits the in-flight Deferred
     * (started by [kickOffProfileLoad]) or starts a fresh load if none is pending.
     */
    suspend fun awaitProfileLoaded(profileId: Int, currency: String) {
        if (profileIndex.containsKey(Pair(profileId, currency))) return
        pendingLoads.computeIfAbsent(Pair(profileId, currency)) {
            cacheScope.async { loadProfile(profileId, currency) }
        }.await()
    }

    suspend fun invalidateProfile(profileId: Int) {
        cache.keys.removeIf { it.first == profileId }
        versionCache.keys.removeIf { it.first == profileId }
        profileIndex.keys.removeIf { it.first == profileId }
        pendingLoads.keys.removeIf { it.first == profileId }
        withContext(Dispatchers.IO) {
            cardCacheDao.clearForProfile(profileId)
        }
    }

    fun invalidateCard(profileId: Int, currency: String, cardId: String) {
        cache.remove(Triple(profileId, currency, cardId))
        versionCache.remove(Triple(profileId, currency, cardId))
        profileIndex[Pair(profileId, currency)]?.remove(cardId)
    }
}
