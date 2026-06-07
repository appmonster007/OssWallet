package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class GlassStyleConfig(
    val blurRadius: Float,
    val lightAlpha: Float,
    val darkAlpha: Float,
    val noiseFactor: Float = 0f,
    val shadowElevation: Float = 8f,
    val borderAlpha: Float = 0.15f,
    val ambientShadowAlpha: Float = 0.10f,
    val spotShadowAlpha: Float = 0.20f,
    val borderWidth: Float = 0.5f
)

@Serializable
data class GlassSettings(
    val standard: GlassStyleConfig = GlassStyleConfig(8f, 0.30f, 0.45f, 0f, 4f, 0.15f, 0.06f, 0.12f, 0.5f),
    val drawer: GlassStyleConfig = GlassStyleConfig(16f, 0.55f, 0.82f, 0f, 16f, 0.20f, 0.08f, 0.18f, 0.5f),
    val pill: GlassStyleConfig = GlassStyleConfig(12f, 0.45f, 0.65f, 0f, 12f, 0.30f, 0.08f, 0.15f, 0.5f),
    val card: GlassStyleConfig = GlassStyleConfig(6f, 0.40f, 0.60f, 0f, 8f, 0.15f, 0.10f, 0.20f, 0.5f),
    val sheet: GlassStyleConfig = GlassStyleConfig(14f, 0.52f, 0.78f, 0f, 12f, 0.15f, 0.08f, 0.15f, 0.5f),
    val popup: GlassStyleConfig = GlassStyleConfig(8f, 0.38f, 0.55f, 0f, 6f, 0.25f, 0.08f, 0.15f, 0.5f)
)
