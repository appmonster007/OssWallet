package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class GlassStyleConfig(
    val blurRadius: Float,
    val lightAlpha: Float,
    val darkAlpha: Float,
    val noiseFactor: Float = 0f
)

@Serializable
data class GlassSettings(
    val standard: GlassStyleConfig = GlassStyleConfig(8f, 0.30f, 0.45f),
    val drawer: GlassStyleConfig = GlassStyleConfig(16f, 0.55f, 0.82f),
    val pill: GlassStyleConfig = GlassStyleConfig(12f, 0.45f, 0.65f),
    val card: GlassStyleConfig = GlassStyleConfig(6f, 0.40f, 0.60f),
    val sheet: GlassStyleConfig = GlassStyleConfig(14f, 0.52f, 0.78f),
    val popup: GlassStyleConfig = GlassStyleConfig(8f, 0.38f, 0.55f)
)
