package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * A wrapper for OpenStreetMap using osmdroid.
 */
@Composable
fun AeonOSMMap(
    modifier: Modifier = Modifier,
    initialPoint: GeoPoint = GeoPoint(0.0, 0.0),
    zoomLevel: Double = 15.0,
    onLocationSelected: (GeoPoint) -> Unit
) {
    // Force placeholder in inspection mode or if class name suggests layoutlib
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current || context.javaClass.name.contains("Layoutlib")
    
    if (isPreview) {
        MapPlaceholder(modifier)
        return
    }

    // Use a state to hold the MapView to avoid multiple creations
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentZoom by remember { mutableStateOf(zoomLevel) }
    var isDragging by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(context) {
        // Initialize osmdroid configuration if not already done
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = context.packageName
        osmConfig.load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        
        val basePath = File(context.cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tiles")

        try {
            val mv = MapView(context)
            // Defensive check for osmdroid initialization
            if (mv.tileProvider != null) {
                mv.setTileSource(TileSourceFactory.MAPNIK)
                mv.setMultiTouchControls(true)
                mv.controller.setZoom(currentZoom)
                mv.controller.setCenter(initialPoint)
                mv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
                // Track zoom level changes from gestures
                mv.addMapListener(DelayedMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean = false
                    override fun onZoom(event: ZoomEvent?): Boolean {
                        event?.zoomLevel?.let { currentZoom = it }
                        return true
                    }
                }, 100))
                
                mapView = mv
            }
        } catch (_: Exception) {
            // Log or handle
        }
    }

    if (mapView == null) {
        MapPlaceholder(modifier)
        return
    }

    val currentMapView = mapView!!

    val marker = remember(currentMapView) {
        Marker(currentMapView).apply {
            position = initialPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker?) {}
                override fun onMarkerDragEnd(marker: Marker?) {
                    isDragging = false
                    marker?.position?.let {
                        onLocationSelected(it as GeoPoint)
                    }
                }
                override fun onMarkerDragStart(marker: Marker?) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isDragging = true
                }
            })
        }
    }

    LaunchedEffect(initialPoint, currentMapView) {
        if ((initialPoint.latitude != 0.0 || initialPoint.longitude != 0.0) && !isDragging) {
            marker.position = initialPoint
            currentMapView.controller.animateTo(initialPoint)
            currentMapView.invalidate()
        }
    }

    DisposableEffect(currentMapView) {
        onDispose {
            currentMapView.onDetach()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentMapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> currentMapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> currentMapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                currentMapView.apply {
                    if (overlays.isEmpty()) {
                        overlays.add(marker)
                    }
                    
                    setOnTouchListener { v, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            v.performClick()
                            val projection = currentMapView.projection
                            val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                            marker.position = geoPoint
                            currentMapView.invalidate()
                            onLocationSelected(geoPoint)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        false // Allow MapView to handle other gestures
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            currentZoom = (currentZoom + 1.0).coerceAtMost(20.0)
                            currentMapView.controller.zoomTo(currentZoom)
                        }
                    )
                },
            update = {
                // Update logic if needed
            }
        )

        // Zoom Controls Overlay
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    RoundedCornerShape(16.dp)
                )
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledIconButton(
                onClick = {
                    currentZoom = (currentZoom + 1.0).coerceAtMost(20.0)
                    currentMapView.controller.zoomTo(currentZoom)
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Zoom In")
            }

            HorizontalDivider(
                modifier = Modifier.width(24.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            FilledIconButton(
                onClick = {
                    currentZoom = (currentZoom - 1.0).coerceAtLeast(1.0)
                    currentMapView.controller.zoomTo(currentZoom)
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out")
            }
        }
        
        // Pin feedback (only visible when dragging)
        if (isDragging) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f), 
                        CircleShape
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Release to set location",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.Map,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
