package com.osscabinet.ossWallet.ui.components.entry

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonOSMMap
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// ─────────────────────────────────────────────────────────────────────────────
// Domain model (private to this file)
// ─────────────────────────────────────────────────────────────────────────────

private data class LocationResult(
    val displayName: String,
    val shortName: String,
    val lat: Double,
    val lon: Double
)

// ─────────────────────────────────────────────────────────────────────────────
// Nominatim helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun nominatimSearch(query: String): List<LocationResult> {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=6&addressdetails=1"
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "OssWallet/1.0")
    conn.connect()
    val body = conn.inputStream.bufferedReader().readText()
    conn.disconnect()

    val arr = JSONArray(body)
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        val displayName = obj.optString("display_name", "")
        val lat = obj.optString("lat", "0").toDoubleOrNull() ?: 0.0
        val lon = obj.optString("lon", "0").toDoubleOrNull() ?: 0.0
        val address = obj.optJSONObject("address")
        val cityName = address?.run {
            optString("city").ifBlank { optString("town").ifBlank { optString("village").ifBlank { optString("suburb") } } }
        } ?: ""
        val shortName = cityName.ifBlank { displayName.split(",").firstOrNull()?.trim() ?: displayName }
        LocationResult(displayName, shortName, lat, lon)
    }
}

private fun nominatimReverse(lat: Double, lon: Double): LocationResult? = try {
    val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json"
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "OssWallet/1.0")
    conn.connect()
    val body = conn.inputStream.bufferedReader().readText()
    conn.disconnect()

    val obj = JSONObject(body)
    val displayName = obj.optString("display_name", "")
    val rawName = obj.optString("name", "")
    val address = obj.optJSONObject("address")
    val cityName = address?.run {
        optString("city").ifBlank { optString("town").ifBlank { optString("village").ifBlank { optString("suburb") } } }
    } ?: ""
    val shortName = rawName.ifBlank { cityName.ifBlank { displayName.split(",").firstOrNull()?.trim() ?: "" } }
    LocationResult(displayName, shortName, lat, lon)
} catch (_: Exception) { null }

@SuppressLint("MissingPermission")
private fun fetchLastLocation(
    context: android.content.Context,
    onResult: (android.location.Location?) -> Unit
) {
    LocationServices.getFusedLocationProviderClient(context)
        .lastLocation
        .addOnSuccessListener { onResult(it) }
        .addOnFailureListener { onResult(null) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AeonLocationSelection(
    gpsLocation: String?,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context        = LocalContext.current
    val clipboard      = LocalClipboardManager.current
    val haptics        = LocalHapticFeedback.current
    val focusManager   = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val parsedCoords = remember(gpsLocation) {
        gpsLocation?.split(",")?.takeIf { it.size == 2 }?.let {
            it[0].toDoubleOrNull()?.let { lat -> it[1].toDoubleOrNull()?.let { lon -> lat to lon } }
        }
    }

    val initialGeoPoint = remember(parsedCoords) {
        parsedCoords?.let { GeoPoint(it.first, it.second) } ?: GeoPoint(20.0, 0.0)
    }

    var mapCenter        by remember { mutableStateOf(initialGeoPoint) }
    var mapZoom          by remember { mutableDoubleStateOf(if (parsedCoords != null) 14.0 else 2.0) }
    var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(if (parsedCoords != null) initialGeoPoint else null) }
    var selectedLocation by remember { mutableStateOf<LocationResult?>(null) }
    var isGeocoding      by remember { mutableStateOf(parsedCoords != null) }

    var searchQuery      by remember { mutableStateOf("") }
    var searchResults    by remember { mutableStateOf<List<LocationResult>>(emptyList()) }
    val recentLocations  = remember { mutableStateListOf<LocationResult>() }
    var isSearchActive   by remember { mutableStateOf(false) }

    var showPanel          by remember { mutableStateOf(parsedCoords != null) }
    var isLocating         by remember { mutableStateOf(false) }
    var isSearchFocused    by remember { mutableStateOf(false) }
    var visibleResultCount by remember { mutableIntStateOf(0) }

    // ── Colors / dims (captured once outside lambdas) ────────────────────────
    val accentColor          = AppColors.accent
    val accentArgb           = accentColor.toArgb()
    val cardSurface          = AppColors.cardSurface
    val contentPrimary       = AppColors.contentPrimary
    val contentSecondary     = AppColors.contentSecondary
    val accentContainer      = AppColors.accentContainer
    val onAccentContainer    = AppColors.onAccentContainer
    val dividerColor         = AppColors.divider
    val dims                 = AeonTheme.dimensions

    val statusBarTop  = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom  = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // ── Initial reverse-geocode ───────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (parsedCoords != null) {
            val result = withContext(Dispatchers.IO) {
                nominatimReverse(parsedCoords.first, parsedCoords.second)
            }
            selectedLocation = result
            isGeocoding = false
        }
    }

    // ── Debounced forward-search ──────────────────────────────────────────────
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) { searchResults = emptyList(); return@LaunchedEffect }
        delay(400)
        searchResults = withContext(Dispatchers.IO) {
            try { nominatimSearch(searchQuery) } catch (_: Exception) { emptyList() }
        }
    }

    // ── Shared: select a geo-point and begin geocoding ────────────────────────
    fun selectPoint(geoPoint: GeoPoint, zoom: Double = 15.0) {
        focusManager.clearFocus()
        searchQuery    = ""
        isSearchActive = false
        selectedGeoPoint = geoPoint
        mapCenter = geoPoint
        mapZoom   = zoom
        selectedLocation = null
        isGeocoding = true
        showPanel   = true
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                nominatimReverse(geoPoint.latitude, geoPoint.longitude)
            }
            selectedLocation = result
            isGeocoding = false
        }
    }

    // ── Permission launcher for GPS ───────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isLocating = true
            fetchLastLocation(context) { loc ->
                isLocating = false
                if (loc != null) selectPoint(GeoPoint(loc.latitude, loc.longitude), zoom = 16.0)
            }
        }
    }

    fun onGpsClick() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (granted) {
            isLocating = true
            fetchLastLocation(context) { loc ->
                isLocating = false
                if (loc != null) selectPoint(GeoPoint(loc.latitude, loc.longitude), zoom = 16.0)
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // ── Staggered search result entrance ─────────────────────────────────────
    LaunchedEffect(searchResults) {
        visibleResultCount = 0
        searchResults.forEachIndexed { index, _ ->
            delay(60L)
            visibleResultCount = index + 1
        }
    }

    // ── Animated state for search bar elevation + GPS button position ─────────
    val searchElevation by animateDpAsState(
        targetValue = if (isSearchFocused) 12.dp else 6.dp,
        label       = "searchElevation"
    )
    val gpsPaddingBottom by animateDpAsState(
        targetValue = if (showPanel) 240.dp + navBarBottom else dims.spacingXl + navBarBottom,
        label       = "gpsPaddingBottom"
    )

    // ── GPS pulse ring animation ──────────────────────────────────────────────
    val gpsRingTransition = rememberInfiniteTransition(label = "gpsRing")
    val gpsRingScale by gpsRingTransition.animateFloat(
        initialValue = 1f, targetValue = 1.65f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "gpsRingScale"
    )
    val gpsRingAlpha by gpsRingTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "gpsRingAlpha"
    )

    val showDropdown = isSearchActive && (searchResults.isNotEmpty() || (searchQuery.isBlank() && recentLocations.isNotEmpty()))
    val searchFocusRequester = remember { FocusRequester() }

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────
    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── 1. Map ────────────────────────────────────────────────────────
            AeonOSMMap(
                modifier     = Modifier.fillMaxSize(),
                center       = mapCenter,
                zoom         = mapZoom,
                selectedPoint = selectedGeoPoint,
                accentArgb   = accentArgb,
                onMapTap     = { selectPoint(it) }
            )

            // ── 2. Top bar: back + search pill ────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        top   = statusBarTop + dims.spacingMd,
                        start = dims.screenPadding,
                        end   = dims.screenPadding
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dims.spacingSm)
            ) {
                // Back
                Surface(
                    modifier      = Modifier.size(44.dp),
                    shape         = CircleShape,
                    color         = cardSurface,
                    shadowElevation = 6.dp,
                    onClick       = onDismiss
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector     = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint            = contentPrimary,
                            modifier        = Modifier.size(22.dp)
                        )
                    }
                }

                // Search pill — elevation and background animate on focus
                Surface(
                    modifier        = Modifier.weight(1f).height(52.dp),
                    shape           = RoundedCornerShape(dims.chipCornerRadius),
                    color           = if (isSearchFocused) AppColors.panelSurface else cardSurface,
                    shadowElevation = searchElevation
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = dims.spacingMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint        = contentSecondary,
                            modifier    = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(dims.spacingSm))
                        BasicTextField(
                            value       = searchQuery,
                            onValueChange = { q ->
                                searchQuery    = q
                                isSearchActive = true
                            },
                            modifier    = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused },
                            singleLine  = true,
                            textStyle   = AppTextStyles.transactionTitle.copy(color = contentPrimary),
                            decorationBox = { inner ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search for a place…",
                                            style = AppTextStyles.transactionTitle.copy(
                                                color = contentSecondary.copy(alpha = 0.55f)
                                            )
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter   = fadeIn(),
                            exit    = fadeOut()
                        ) {
                            IconButton(
                                onClick  = { searchQuery = ""; isSearchActive = false; focusManager.clearFocus() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp),
                                    tint     = contentSecondary
                                )
                            }
                        }
                    }
                }
            }

            // ── 3. Search results / recents dropdown ──────────────────────────
            AnimatedVisibility(
                visible  = showDropdown,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        top   = statusBarTop + dims.spacingMd + 52.dp + dims.spacingXs,
                        start = dims.screenPadding + 44.dp + dims.spacingSm,
                        end   = dims.screenPadding
                    ),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit  = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Surface(
                    shape           = RoundedCornerShape(dims.containerCornerRadius),
                    color           = cardSurface,
                    shadowElevation = 12.dp,
                    modifier        = Modifier.heightIn(max = 300.dp)
                ) {
                    LazyColumn(contentPadding = PaddingValues(vertical = dims.spacingXs)) {
                        if (searchQuery.isBlank() && recentLocations.isNotEmpty()) {
                            item {
                                Text(
                                    "RECENT",
                                    style    = AppTextStyles.sectionOverline,
                                    color    = contentSecondary,
                                    modifier = Modifier.padding(
                                        horizontal = dims.spacingLg,
                                        vertical   = dims.spacingXs
                                    )
                                )
                            }
                            items(recentLocations.take(5), key = { idx -> "recent_$idx" }) { loc ->
                                LocationRow(loc, contentPrimary, contentSecondary, cardSurface) {
                                    selectPoint(GeoPoint(loc.lat, loc.lon))
                                    selectedLocation = loc
                                    isGeocoding = false
                                }
                            }
                        } else if (searchResults.isNotEmpty()) {
                            itemsIndexed(searchResults, key = { idx, _ -> "search_$idx" }) { idx, loc ->
                                AnimatedVisibility(
                                    visible = idx < visibleResultCount,
                                    enter   = fadeIn(tween(180)) + expandVertically(tween(180))
                                ) {
                                    LocationRow(loc, contentPrimary, contentSecondary, cardSurface) {
                                        selectPoint(GeoPoint(loc.lat, loc.lon))
                                        selectedLocation = loc
                                        isGeocoding = false
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. GPS button (bottom-end, above the panel) — animated position ─
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = dims.spacingLg, bottom = gpsPaddingBottom)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing ring while locating
                if (isLocating) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = gpsRingScale
                                scaleY = gpsRingScale
                                alpha  = gpsRingAlpha
                            }
                    ) {
                        drawCircle(
                            color  = accentColor,
                            radius = size.minDimension / 2f,
                            style  = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                Surface(
                    modifier        = Modifier.size(48.dp),
                    shape           = CircleShape,
                    color           = cardSurface,
                    shadowElevation = 6.dp,
                    onClick         = ::onGpsClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color       = accentColor
                            )
                        } else {
                            Icon(
                                Icons.Rounded.GpsFixed,
                                contentDescription = "My location",
                                tint     = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // ── 5. Location panel (scrim-free, slides up from bottom) ─────────
            AnimatedVisibility(
                visible  = showPanel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(topStart = dims.dialogCornerRadius, topEnd = dims.dialogCornerRadius),
                    color           = cardSurface,
                    shadowElevation = 20.dp,
                    tonalElevation  = 2.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Drag handle + dismiss row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = dims.spacingMd),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 5.dp)
                                    .background(dividerColor.copy(alpha = 0.45f), CircleShape)
                            )
                            // Small × to dismiss panel without picking a location
                            IconButton(
                                onClick  = { showPanel = false; selectedGeoPoint = null; selectedLocation = null },
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = dims.spacingMd).size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = contentSecondary, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Content area — transitions between loading and loaded
                        AnimatedContent(
                            targetState = isGeocoding to selectedLocation,
                            transitionSpec = {
                                (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
                            },
                            label = "panelContent"
                        ) { (geocoding, location) ->
                            if (geocoding || location == null) {
                                // Shimmer skeleton — two text lines
                                val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
                                val shimmerX by shimmerTransition.animateFloat(
                                    initialValue  = 0f,
                                    targetValue   = 800f,
                                    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                                    label         = "shimmerX"
                                )
                                val shimmerBrush = Brush.linearGradient(
                                    colors = listOf(AppColors.insetSurface, AppColors.divider, AppColors.insetSurface),
                                    start  = Offset(shimmerX - 400f, 0f),
                                    end    = Offset(shimmerX, 0f)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = dims.screenPadding, vertical = dims.spacingLg),
                                    verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.68f)
                                            .height(16.dp)
                                            .background(shimmerBrush, RoundedCornerShape(8.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.44f)
                                            .height(12.dp)
                                            .background(shimmerBrush, RoundedCornerShape(6.dp))
                                    )
                                }
                            } else {
                                LocationPanelBody(
                                    location          = location,
                                    accentContainer   = accentContainer,
                                    onAccentContainer = onAccentContainer,
                                    contentPrimary    = contentPrimary,
                                    contentSecondary  = contentSecondary,
                                    dividerColor      = dividerColor,
                                    onCopy = {
                                        clipboard.setText(AnnotatedString("${location.lat}, ${location.lon}"))
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    }
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = dims.screenPadding),
                            color     = dividerColor.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )

                        // Confirm button
                        Button(
                            onClick = {
                                val loc = selectedLocation ?: return@Button
                                if (recentLocations.none { it.lat == loc.lat && it.lon == loc.lon }) {
                                    recentLocations.add(0, loc)
                                    if (recentLocations.size > 5) recentLocations.removeAt(recentLocations.size - 1)
                                }
                                onLocationSelected("${loc.lat},${loc.lon}")
                                onDismiss()
                            },
                            enabled  = selectedLocation != null && !isGeocoding,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dims.screenPadding, vertical = dims.spacingLg),
                            shape    = RoundedCornerShape(dims.buttonCornerRadius)
                        ) {
                            Text(
                                "Confirm Location",
                                style    = AppTextStyles.cardSectionTitle.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = dims.spacingXs)
                            )
                        }

                        Spacer(Modifier.height(navBarBottom + dims.spacingXs))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel body — location name, coord chips, copy action
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocationPanelBody(
    location: LocationResult,
    accentContainer: Color,
    onAccentContainer: Color,
    contentPrimary: Color,
    contentSecondary: Color,
    dividerColor: Color,
    onCopy: () -> Unit
) {
    val dims = AeonTheme.dimensions

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenPadding)
            .padding(top = dims.spacingMd, bottom = dims.spacingLg)
    ) {
        // Place name — visually dominant anchor
        if (location.shortName.isNotBlank()) {
            Text(
                text     = location.shortName,
                style    = AppTextStyles.heroAmount.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color    = contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(dims.spacingXs))
        }

        // Full address — caption style, muted
        Text(
            text     = location.displayName,
            style    = AppTextStyles.caption.copy(fontSize = 12.sp),
            color    = contentSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(dims.spacingLg))

        // Coord chips + copy button on one row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacingXs)
        ) {
            CoordChip("Lat", location.lat, accentContainer, onAccentContainer)
            CoordChip("Lon", location.lon, accentContainer, onAccentContainer)

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick      = onCopy,
                contentPadding = PaddingValues(horizontal = dims.spacingMd, vertical = 0.dp)
            ) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(dims.spacingXs))
                Text("Copy", style = AppTextStyles.metaLabel.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun CoordChip(
    label: String,
    value: Double,
    containerColor: Color,
    contentColor: Color
) {
    val dims = AeonTheme.dimensions
    Surface(
        shape = RoundedCornerShape(dims.chipCornerRadius),
        color = containerColor
    ) {
        Row(
            modifier  = Modifier.padding(horizontal = dims.spacingMd, vertical = dims.spacingXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dims.spacingXs)
        ) {
            Icon(
                Icons.Rounded.Explore,
                contentDescription = null,
                tint     = contentColor,
                modifier = Modifier.size(AeonTheme.dimensions.iconSizeXs)
            )
            Text(
                "$label: ${"%.5f".format(value)}",
                style = AppTextStyles.metaLabel,
                color = contentColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dropdown row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocationRow(
    location: LocationResult,
    contentPrimary: Color,
    contentSecondary: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(location.shortName, style = AppTextStyles.transactionTitle, color = contentPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(location.displayName, style = AppTextStyles.transactionSubtitle, color = contentSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = contentSecondary, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors   = ListItemDefaults.colors(containerColor = containerColor)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@ThemePreviews
@Composable
fun AeonLocationSelectionEmptyPreview() {
    OssWalletTheme {
        AeonLocationSelection(gpsLocation = null, onLocationSelected = {}, onDismiss = {})
    }
}

@ThemePreviews
@Composable
fun AeonLocationSelectionWithPinPreview() {
    OssWalletTheme {
        AeonLocationSelection(gpsLocation = "37.7749,-122.4194", onLocationSelected = {}, onDismiss = {})
    }
}

@ThemePreviews
@Composable
fun LocationPanelPreview() {
    OssWalletTheme {
        Surface(color = AppColors.cardSurface) {
            LocationPanelBody(
                location = LocationResult(
                    displayName  = "Ferry Building, 1 Ferry Building, San Francisco, CA 94111, United States",
                    shortName    = "Ferry Building",
                    lat          = 37.79520,
                    lon          = -122.39390
                ),
                accentContainer   = AppColors.accentContainer,
                onAccentContainer = AppColors.onAccentContainer,
                contentPrimary    = AppColors.contentPrimary,
                contentSecondary  = AppColors.contentSecondary,
                dividerColor      = AppColors.divider,
                onCopy            = {}
            )
        }
    }
}
