package com.osscabinet.ossWallet.data.di

import com.osscabinet.ossWallet.domain.model.util.BigDecimalSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import javax.inject.Singleton

/**
 * Single source of truth for kotlinx.serialization configuration.
 *
 * Provides one app-wide [Json] that registers the contextual [BigDecimalSerializer]
 * so every consumer serializes `BigDecimal` consistently. `explicitNulls` is left at
 * its default (true) so encoded output stays byte-compatible with existing backups;
 * decode is made lenient (`ignoreUnknownKeys` + `coerceInputValues`) for forward
 * compatibility.
 */
@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
        }
    }
}
