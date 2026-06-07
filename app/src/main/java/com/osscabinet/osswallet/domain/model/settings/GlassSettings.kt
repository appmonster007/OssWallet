package com.osscabinet.ossWallet.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class GlassProperties(
    val blurRadius: Float,
    val lightAlpha: Float,
    val darkAlpha: Float,
    val noiseFactor: Float = 0f,
)

@Serializable
data class SurfaceProperties(
    val shadowElevation: Float = 8f,
    val borderAlpha: Float = 0.15f,
    val ambientShadowAlpha: Float = 0.10f,
    val spotShadowAlpha: Float = 0.20f,
    val borderWidth: Float = 0.5f,
)

@Serializable
data class GlassStyleConfig(
    val glass: GlassProperties,
    val surface: SurfaceProperties,
)

@Serializable
data class GlassSettings(
    val standard: GlassStyleConfig = GlassStyleConfig(
        glass = GlassProperties(8f, 0.30f, 0.45f),
        surface = SurfaceProperties(4f, 0.15f, 0.06f, 0.12f, 0.5f)
    ),
    val drawer: GlassStyleConfig = GlassStyleConfig(
        glass = GlassProperties(16f, 0.55f, 0.82f),
        surface = SurfaceProperties(16f, 0.20f, 0.08f, 0.18f, 0.5f)
    ),
    val pill: GlassStyleConfig = GlassStyleConfig(
        glass = GlassProperties(12f, 0.45f, 0.65f),
        surface = SurfaceProperties(12f, 0.30f, 0.08f, 0.15f, 0.5f)
    ),
    val card: GlassStyleConfig = GlassStyleConfig(
        glass = GlassProperties(6f, 0.40f, 0.60f),
        surface = SurfaceProperties(8f, 0.15f, 0.10f, 0.20f, 0.5f)
    ),
    val sheet: GlassStyleConfig = GlassStyleConfig(
        glass = GlassProperties(14f, 0.52f, 0.78f),
        surface = SurfaceProperties(12f, 0.15f, 0.08f, 0.15f, 0.5f)
    ),
    val popup: GlassStyleConfig = GlassStyleConfig(
        glass = GlassProperties(8f, 0.38f, 0.55f),
        surface = SurfaceProperties(6f, 0.25f, 0.08f, 0.15f, 0.5f)
    ),
    val item: GlassStyleConfig = GlassStyleConfig(
        glass = GlassProperties(4f, 0.25f, 0.50f),
        surface = SurfaceProperties(3f, 0.10f, 0.05f, 0.10f, 0.5f)
    )
)
