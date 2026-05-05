package com.osscabinet.ossWallet.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
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
                mv.controller.setZoom(zoomLevel)
                mv.controller.setCenter(initialPoint)
                mv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
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
        }
    }

    LaunchedEffect(initialPoint, currentMapView) {
        if (initialPoint.latitude != 0.0 || initialPoint.longitude != 0.0) {
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

    AndroidView(
        factory = {
            currentMapView.apply {
                if (overlays.isEmpty()) {
                    overlays.add(marker)
                }
                
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                        val projection = currentMapView.projection
                        val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                        marker.position = geoPoint
                        currentMapView.invalidate()
                        onLocationSelected(geoPoint)
                    }
                    true
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = {
            // Update logic if needed
        }
    )
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
