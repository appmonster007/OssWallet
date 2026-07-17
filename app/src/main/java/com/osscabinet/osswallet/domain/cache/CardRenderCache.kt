package com.osscabinet.ossWallet.domain.cache

import com.osscabinet.ossWallet.data.local.dao.CardCacheDao
import com.osscabinet.ossWallet.data.local.entity.CardCacheEntity
import com.osscabinet.ossWallet.domain.card.CardRenderData
import com.osscabinet.ossWallet.domain.card.ScreenPersistenceEnvelope
import java.util.concurrent.ConcurrentHashMap
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Global cache for rendered dashboard cards and dashboard persistence envelopes.
 * Keyed by (profileId, currency, cardId) for granular invalidation.
 * Supports persistent storage in Room with version tracking via transactions' lastUpdated.
 */
@Singleton
class CardRenderCache @Inject constructor(
    private val cardCacheDao: CardCacheDao,
    private val json: Json
) {
    private val cardCache = ConcurrentHashMap<Triple<Int, String, String>, CardRenderData>()
    private val envelopeCache = ConcurrentHashMap<Triple<Int, String, String>, ScreenPersistenceEnvelope>()
    private val versionCache = ConcurrentHashMap<Triple<Int, String, String>, Long>()

    // Secondary indices for profile snapshots
    private val cardProfileIndex = ConcurrentHashMap<Pair<Int, String>, ConcurrentHashMap<String, CardRenderData>>()
    private val envelopeProfileIndex = ConcurrentHashMap<Pair<Int, String>, ConcurrentHashMap<String, ScreenPersistenceEnvelope>>()

    fun getCard(profileId: Int, currency: String, cardId: String): CardRenderData? =
        cardCache[Triple(profileId, currency, cardId)]

    fun getEnvelope(profileId: Int, currency: String, key: String): ScreenPersistenceEnvelope? =
        envelopeCache[Triple(profileId, currency, key)]

    fun getVersion(profileId: Int, currency: String, key: String): Long? =
        versionCache[Triple(profileId, currency, key)]

    /**
     * Version-aware setter that persists to DB for CardRenderData.
     */
    suspend fun persist(profileId: Int, currency: String, cardId: String, data: CardRenderData, version: Long) {
        withContext(Dispatchers.IO) {
            cardCache[Triple(profileId, currency, cardId)] = data
            versionCache[Triple(profileId, currency, cardId)] = version
            cardProfileIndex.compute(Pair(profileId, currency)) { _, existing ->
                (existing ?: ConcurrentHashMap()).also { it[cardId] = data }
            }

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
     * Version-aware setter that persists to DB for ScreenPersistenceEnvelope.
     */
    suspend fun persistEnvelope(profileId: Int, currency: String, key: String, envelope: ScreenPersistenceEnvelope, version: Long) {
        withContext(Dispatchers.IO) {
            envelopeCache[Triple(profileId, currency, key)] = envelope
            versionCache[Triple(profileId, currency, key)] = version
            envelopeProfileIndex.compute(Pair(profileId, currency)) { _, existing ->
                (existing ?: ConcurrentHashMap()).also { it[key] = envelope }
            }

            val jsonStr = json.encodeToString(envelope)
            cardCacheDao.insert(
                CardCacheEntity(
                    profileId = profileId,
                    currency = currency,
                    cardId = key,
                    dataJson = jsonStr,
                    version = version
                )
            )
        }
    }

    /**
     * Loads the entire profile cache from DB into memory.
     * Distinguishes between CardRenderData and ScreenPersistenceEnvelope using heuristic on key or trial decoding.
     */
    suspend fun loadProfile(profileId: Int, currency: String): Boolean = withContext(Dispatchers.IO) {
        val entities = cardCacheDao.getCacheForProfile(profileId, currency)
        if (entities.isEmpty()) return@withContext false

        entities.forEach { entity ->
            try {
                if (entity.cardId.startsWith("dashboard_") || entity.cardId.endsWith("_snapshot") || entity.cardId.endsWith("_feed")) {
                    val envelope = json.decodeFromString<ScreenPersistenceEnvelope>(entity.dataJson)
                    envelopeCache[Triple(profileId, currency, entity.cardId)] = envelope
                    envelopeProfileIndex.compute(Pair(profileId, currency)) { _, existing ->
                        (existing ?: ConcurrentHashMap()).also { it[entity.cardId] = envelope }
                    }
                } else {
                    val data = json.decodeFromString<CardRenderData>(entity.dataJson)
                    cardCache[Triple(profileId, currency, entity.cardId)] = data
                    cardProfileIndex.compute(Pair(profileId, currency)) { _, existing ->
                        (existing ?: ConcurrentHashMap()).also { it[entity.cardId] = data }
                    }
                }
                versionCache[Triple(profileId, currency, entity.cardId)] = entity.version
            } catch (e: Exception) {
                // Handle or log deserialization error
            }
        }
        true
    }

    fun getProfileCardSnapshot(profileId: Int, currency: String): Map<String, CardRenderData> =
        cardProfileIndex[Pair(profileId, currency)]?.toMap() ?: emptyMap()

    fun getProfileEnvelopeSnapshot(profileId: Int, currency: String): Map<String, ScreenPersistenceEnvelope> =
        envelopeProfileIndex[Pair(profileId, currency)]?.toMap() ?: emptyMap()

    suspend fun invalidateProfile(profileId: Int) {
        cardCache.keys.removeIf { it.first == profileId }
        envelopeCache.keys.removeIf { it.first == profileId }
        versionCache.keys.removeIf { it.first == profileId }
        cardProfileIndex.keys.removeIf { it.first == profileId }
        envelopeProfileIndex.keys.removeIf { it.first == profileId }
        withContext(Dispatchers.IO) {
            cardCacheDao.clearForProfile(profileId)
        }
    }

    fun invalidateCard(profileId: Int, currency: String, cardId: String) {
        cardCache.remove(Triple(profileId, currency, cardId))
        versionCache.remove(Triple(profileId, currency, cardId))
        cardProfileIndex[Pair(profileId, currency)]?.remove(cardId)
    }
}
