package com.osscabinet.ossWallet.ui.components

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Pin bitmap builder — pure function, safe to call on any thread
// ─────────────────────────────────────────────────────────────────────────────

private fun buildPinBitmap(accentArgb: Int): Bitmap {
    val size = 88
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)

    // Soft shadow below tail tip
    cv.drawCircle(
        size / 2f, size - 8f, 14f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(60, 0, 0, 0)
            maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
        }
    )

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentArgb
        style = Paint.Style.FILL
    }

    // Pin head circle
    val hCx = size / 2f
    val hCy = 28f
    val hR = 22f
    cv.drawCircle(hCx, hCy, hR, fillPaint)

    // Inner white ring for depth
    cv.drawCircle(
        hCx, hCy, hR - 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(40, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
    )

    // Teardrop tail pointing down
    cv.drawPath(
        Path().apply {
            moveTo(hCx - 11f, hCy + hR - 5f)
            lineTo(hCx + 11f, hCy + hR - 5f)
            lineTo(hCx, size - 10f)
            close()
        },
        fillPaint
    )

    // High-contrast center dot
    cv.drawCircle(
        hCx, hCy, 7f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
    )

    return bmp
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AeonOSMMap(
    modifier: Modifier = Modifier,
    center: GeoPoint,
    zoom: Double = 14.0,
    selectedPoint: GeoPoint? = null,
    accentArgb: Int = android.graphics.Color.parseColor("#004D5F"),
    onMapTap: (GeoPoint) -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val screenBgColor = AppColors.screenBackground
    val cardSurfaceColor = AppColors.cardSurface
    val contentPrimaryColor = AppColors.contentPrimary
    val dividerColor = AppColors.divider

    val screenBgArgb = screenBgColor.toArgb()
    val cardSurfaceArgb = cardSurfaceColor.toArgb()
    val contentPrimaryArgb = contentPrimaryColor.toArgb()

    // ── Stable MapView ────────────────────────────────────────────────────────
    val mapView = remember(context, isPreview) {
        if (isPreview) null else {
            Configuration.getInstance().userAgentValue = context.packageName
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)   // pinch-to-zoom + double-tap zoom
                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            }
        }
    }

    // ── Compass rotation (polled, not observed) ───────────────────────────────
    var mapRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(mapView) {
        if (mapView == null) return@LaunchedEffect
        while (true) {
            delay(80)
            mapRotation = mapView.mapOrientation
        }
    }

    // ── Pin drop bounce animation — driven by selectedPoint identity ──────────
    // A version counter increments each time selectedPoint changes to a non-null
    // value. animateFloatAsState then bounces from 0 → 1 using a spring, giving
    // the pin its characteristic drop-and-settle feel.
    var pinDropVersion by remember { mutableStateOf(0) }
    var pinVisible by remember { mutableStateOf(selectedPoint != null) }

    LaunchedEffect(selectedPoint) {
        if (selectedPoint != null) {
            pinDropVersion++
            pinVisible = true
        } else {
            pinVisible = false
        }
    }

    val pinScale by animateFloatAsState(
        targetValue = if (pinVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pinScale"
    )

    // ── Sync center / zoom from caller (only when they change) ────────────────
    LaunchedEffect(center, zoom, mapView) {
        mapView?.controller?.animateTo(center)
        mapView?.controller?.setZoom(zoom)
    }

    // ── Gesture overlays — set up ONCE, never recreated ───────────────────────
    val currentOnMapTap by rememberUpdatedState(onMapTap)

    LaunchedEffect(mapView) {
        if (mapView == null) return@LaunchedEffect

        // Two-finger rotation
        val rotationOverlay = RotationGestureOverlay(mapView).apply { isEnabled = true }
        mapView.overlays.add(0, rotationOverlay)

        // Single-tap → onMapTap (always calls latest lambda via rememberUpdatedState)
        val tapReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { currentOnMapTap(it) }
                return true
            }

            override fun longPressHelper(p: GeoPoint?) = false
        }
        mapView.overlays.add(1, MapEventsOverlay(tapReceiver))
    }

    // ── Pin marker — reuse stable instance, only move / redraw on change ──────
    val markerRef = remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(selectedPoint, accentArgb, mapView) {
        if (mapView == null) return@LaunchedEffect

        if (selectedPoint == null) {
            markerRef.value?.let { mapView.overlays.remove(it) }
            markerRef.value = null
            mapView.invalidate()
            return@LaunchedEffect
        }

        val drawable = BitmapDrawable(context.resources, buildPinBitmap(accentArgb))
        val existing = markerRef.value
        if (existing != null) {
            existing.position = selectedPoint
            existing.icon = drawable
        } else {
            val marker = Marker(mapView).apply {
                position = selectedPoint
                icon = drawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setInfoWindow(null)
            }
            mapView.overlays.add(marker)
            markerRef.value = marker
        }
        mapView.invalidate()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    DisposableEffect(mapView) {
        if (mapView == null) return@DisposableEffect onDispose {}
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(modifier = modifier) {

        if (isPreview) {
            // ── Preview placeholder (tile grid + pin) ─────────────────────────
            val tileColor = AppColors.insetSurface
            val gridColor = AppColors.divider
            val accentC = Color(accentArgb)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = tileColor)
                val tile = 60.dp.toPx()
                if (tile > 0f) {
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                        x += tile
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                        y += tile
                    }
                }
                if (selectedPoint != null) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(Color.Black.copy(alpha = 0.18f), 13f, Offset(cx, cy + 24f))
                    drawCircle(accentC, 22f, Offset(cx, cy - 22f))
                    drawPath(androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx - 11f, cy - 2f); lineTo(cx + 11f, cy - 2f); lineTo(
                        cx,
                        cy + 20f
                    ); close()
                    }, accentC)
                    drawCircle(Color.White, 7f, Offset(cx, cy - 22f))
                }
            }
        } else {
            // ── Live osmdroid map ─────────────────────────────────────────────
            AndroidView(
                factory = { mapView!! },
                modifier = Modifier.fillMaxSize(),
                update = { /* all state sync via LaunchedEffect above */ }
            )
        }

        // ── Pin scale overlay — Compose-driven bounce at screen center ────────
        // The real osmdroid marker handles geospatial positioning. This overlay
        // renders on top during the drop animation (scale 0→1 with spring overshoot)
        // and becomes invisible once pinScale reaches 1f. The overlay is clipped
        // to its Box so it never bleeds outside the map bounds.
        if (selectedPoint != null && pinScale < 0.98f) {
            val accentC = Color(accentArgb)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = pinScale
                        scaleY = pinScale
                        // Anchor the scale transform to the center of the map
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Shadow
                drawCircle(Color.Black.copy(alpha = 0.18f * pinScale), 13f, Offset(cx, cy + 24f))
                // Pin head
                drawCircle(accentC, 22f, Offset(cx, cy - 22f))
                // Teardrop tail
                drawPath(androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - 11f, cy - 2f)
                    lineTo(cx + 11f, cy - 2f)
                    lineTo(cx, cy + 20f)
                    close()
                }, accentC)
                // Center dot
                drawCircle(Color.White, 7f, Offset(cx, cy - 22f))
            }
        }

        // ── Top scrim (behind search / back bar) ──────────────────────────────
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(screenBgArgb).copy(alpha = 0.72f), Color.Transparent),
                            startY = 0f, endY = 130.dp.toPx()
                        )
                    )
                }
        )

        // ── Bottom scrim (behind panel) ───────────────────────────────────────
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter)
                .drawBehind {
                    val h = 140.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, Color(screenBgArgb).copy(alpha = 0.72f)),
                            startY = size.height - h, endY = size.height
                        )
                    )
                }
        )

        // ── Compass — top-right, below the search bar — only when map is rotated
        // Follows Google Maps / Apple Maps convention: the compass fades in when
        // the map is tilted off north and disappears when it's reset, avoiding
        // visual clutter on the default north-up view.
        AnimatedVisibility(
            visible = abs(mapRotation) > 2f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = AeonTheme.dimensions.spacingXl + 60.dp,
                    end = AeonTheme.dimensions.spacingLg
                ),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                color = Color(cardSurfaceArgb),
                shadowElevation = 4.dp,
                border = BorderStroke(0.5.dp, dividerColor),
                onClick = { mapView?.mapOrientation = 0f }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = "Compass — tap to reset north",
                        tint = Color(contentPrimaryArgb),
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = -mapRotation }
                    )
                }
            }
        }

        // ── Zoom +/− — right edge, vertically centered ────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = AeonTheme.dimensions.spacingLg)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(cardSurfaceArgb),
                shadowElevation = 4.dp,
                border = BorderStroke(0.5.dp, dividerColor),
                onClick = { mapView?.controller?.zoomIn() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Zoom in",
                        tint = Color(contentPrimaryArgb),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.height(AeonTheme.dimensions.spacingXs))
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(cardSurfaceArgb),
                shadowElevation = 4.dp,
                border = BorderStroke(0.5.dp, dividerColor),
                onClick = { mapView?.controller?.zoomOut() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Remove,
                        contentDescription = "Zoom out",
                        tint = Color(contentPrimaryArgb),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@ThemePreviews
@Composable
fun AeonOSMMapPreview() {
    OssWalletTheme {
        Box(
            modifier = Modifier
                .width(360.dp)
                .height(500.dp)
        ) {
            AeonOSMMap(
                modifier = Modifier.fillMaxSize(),
                center = GeoPoint(37.7749, -122.4194),
                zoom = 14.0,
                selectedPoint = GeoPoint(37.7749, -122.4194)
            )
        }
    }
}
