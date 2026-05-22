package com.osscabinet.ossWallet.ui.components.entry

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.osscabinet.ossWallet.ui.components.AeonFullScreenDialog
import com.osscabinet.ossWallet.ui.components.AeonOSMMap
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.ThemePreviews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AeonLocationSelection(
    currentCoords: Pair<Double, Double>?,
    onLocationSelected: (Pair<Double, Double>?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Location"
) {
    val context = LocalContext.current
    var selectedCoords by remember { mutableStateOf(currentCoords) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<LocationSearchResult>()) }
    var searchHistory by remember { mutableStateOf(emptyList<LocationSearchResult>()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchActive by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    fun getCurrentLocation() {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        if (locationPermissionState.status.isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        selectedCoords = it.latitude to it.longitude
                    }
                }
            } catch (_: SecurityException) {}
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    suspend fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        searchError = null
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1")
                    .header("User-Agent", "OssWallet-Android-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val jsonArray = JSONArray(body)
                        val results = mutableListOf<LocationSearchResult>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val address = obj.optJSONObject("address")
                            
                            val mainName = address?.let { addr ->
                                addr.optString("road").takeIf { it.isNotBlank() }
                                    ?: addr.optString("amenity").takeIf { it.isNotBlank() }
                                    ?: addr.optString("building").takeIf { it.isNotBlank() }
                                    ?: addr.optString("house_number").takeIf { it.isNotBlank() }
                                    ?: obj.getString("display_name").split(",").firstOrNull() ?: ""
                            } ?: obj.getString("display_name").split(",").firstOrNull() ?: ""
                            
                            val secondaryName = obj.getString("display_name")
                                .replaceFirst(mainName, "")
                                .trim().trimStart(',')
                                .trim()

                            results.add(
                                LocationSearchResult(
                                    display = obj.getString("display_name"),
                                    mainText = mainName,
                                    secondaryText = secondaryName,
                                    lat = obj.getDouble("lat"),
                                    lon = obj.getDouble("lon")
                                )
                            )
                        }
                        withContext(Dispatchers.Main) {
                            searchResults = results
                            isSearching = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            searchError = "Search failed"
                            isSearching = false
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    searchError = "Network error"
                    isSearching = false
                }
            }
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&addressdetails=1")
                    .header("User-Agent", "OssWallet-Android-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val displayName = json.optString("display_name", "")
                        withContext(Dispatchers.Main) {
                            selectedAddress = displayName
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (selectedCoords == null) {
            if (locationPermissionState.status.isGranted) {
                getCurrentLocation()
            } else {
                locationPermissionState.launchPermissionRequest()
            }
        }
    }

    LaunchedEffect(selectedCoords) {
        selectedCoords?.let { (lat, lon) ->
            reverseGeocode(lat, lon)
        } ?: run {
            selectedAddress = null
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            delay(600)
            performSearch(searchQuery)
        } else {
            searchResults = emptyList()
            searchError = null
        }
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        AeonLocationSelectionContent(
            selectedCoords = selectedCoords,
            selectedAddress = selectedAddress,
            searchQuery = searchQuery,
            searchResults = searchResults,
            searchHistory = searchHistory,
            isSearching = isSearching,
            searchError = searchError,
            searchActive = searchActive,
            onBack = onDismiss,
            onSearchQueryChange = { searchQuery = it },
            onSearchActiveChange = { searchActive = it },
            onSearchResultClick = { result ->
                selectedCoords = result.lat to result.lon
                searchActive = false
                searchQuery = result.display
                searchHistory = (listOf(result) + searchHistory.filter { it.display != result.display }).take(5)
            },
            onClearHistory = { searchHistory = emptyList() },
            onManualPin = { geoPoint ->
                selectedCoords = geoPoint.latitude to geoPoint.longitude
                searchQuery = ""
            },
            onGetCurrentLocation = { getCurrentLocation() },
            onClear = { selectedCoords = null; searchQuery = "" },
            onConfirm = {
                onLocationSelected(selectedCoords)
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonLocationSelectionContent(
    selectedCoords: Pair<Double, Double>?,
    selectedAddress: String?,
    searchQuery: String,
    searchResults: List<LocationSearchResult>,
    searchHistory: List<LocationSearchResult>,
    isSearching: Boolean,
    searchError: String?,
    searchActive: Boolean,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchResultClick: (LocationSearchResult) -> Unit,
    onClearHistory: () -> Unit,
    onManualPin: (GeoPoint) -> Unit,
    onGetCurrentLocation: () -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // [1] Immersive Map Background
        AeonOSMMap(
            initialPoint = GeoPoint(
                selectedCoords?.first ?: 0.0,
                selectedCoords?.second ?: 0.0
            ),
            onLocationSelected = onManualPin,
            modifier = Modifier.fillMaxSize()
        )

        // Top Shadow Scrim for control legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // [2] Floating Search Bar (Aeon Style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(2f)
        ) {
            DockedSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = { /* Handled by LaunchedEffect */ },
                active = searchActive,
                onActiveChange = onSearchActiveChange,
                placeholder = { Text("Search location...", style = AppTextStyles.listItemTitle.copy(color = AppColors.contentSecondary)) },
                leadingIcon = {
                    IconButton(onClick = { if (searchActive) onSearchActiveChange(false) else onBack() }) {
                        Icon(
                            if (searchActive) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.primary
                        )
                    }
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = AppColors.primary
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = AppColors.contentSecondary)
                        }
                    }
                },
                colors = SearchBarDefaults.colors(
                    containerColor = AppColors.panelSurface,
                    dividerColor = AppColors.divider
                ),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                val displayResults = if (searchQuery.isEmpty()) searchHistory else searchResults
                
                if (searchError != null) {
                    Text(
                        text = searchError,
                        color = AppColors.error,
                        style = AppTextStyles.caption,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "RECENT",
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = AppColors.primary
                        )
                        TextButton(onClick = onClearHistory) {
                            Text("Clear All", style = AppTextStyles.caption.copy(fontWeight = FontWeight.Bold), color = AppColors.error)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayResults) { result ->
                        ListItem(
                            headlineContent = { 
                                Text(
                                    result.mainText, 
                                    style = AppTextStyles.listItemTitle.copy(fontWeight = FontWeight.Bold)
                                ) 
                            },
                            supportingContent = {
                                Text(
                                    result.secondaryText,
                                    style = AppTextStyles.caption,
                                    color = AppColors.contentSecondary
                                )
                            },
                            modifier = Modifier.clickable { onSearchResultClick(result) },
                            leadingContent = { 
                                Surface(
                                    shape = CircleShape,
                                    color = if (searchQuery.isEmpty()) AppColors.secondaryContainer else AppColors.accentContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (searchQuery.isEmpty()) Icons.Rounded.History else Icons.Rounded.Place, 
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (searchQuery.isEmpty()) AppColors.onSecondaryContainer else AppColors.onAccentContainer
                                        )
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }

        // [3] My Location FAB
        FloatingActionButton(
            onClick = onGetCurrentLocation,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .padding(bottom = if (selectedCoords != null) 176.dp else 24.dp),
            containerColor = AppColors.panelSurface,
            contentColor = AppColors.primary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = "My Location")
        }

        // [4] Floating Action Card (Aeon Style)
        AnimatedVisibility(
            visible = selectedCoords != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = AppColors.panelSurface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AppColors.accentContainer,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Place,
                                    contentDescription = null,
                                    tint = AppColors.onAccentContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedAddress?.split(",")?.firstOrNull() ?: "Selected Location",
                                style = AppTextStyles.screenTitle.copy(fontSize = 18.sp),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedAddress?.substringAfter(",")?.trim() ?: "%.6f, %.6f".format(selectedCoords?.first ?: 0.0, selectedCoords?.second ?: 0.0),
                                style = AppTextStyles.caption,
                                color = AppColors.contentSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = onClear) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = AppColors.error)
                        }
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Confirm Location", 
                            style = AppTextStyles.managementItemTitle
                        )
                    }
                }
            }
        }
    }
}

data class LocationSearchResult(
    val display: String,
    val mainText: String,
    val secondaryText: String,
    val lat: Double,
    val lon: Double
)

@ThemePreviews
@Composable
fun AeonLocationSelectionContentPreview() {
    OssWalletTheme {
        AeonLocationSelectionContent(
            selectedCoords = 1.23456 to 7.89012,
            selectedAddress = "123 Main St, New York, NY",
            searchQuery = "",
            searchResults = emptyList(),
            searchHistory = listOf(
                LocationSearchResult("Times Square", "Times Square", "New York, NY", 40.7580, -73.9855)
            ),
            isSearching = false,
            searchError = null,
            searchActive = false,
            onBack = {},
            onSearchQueryChange = {},
            onSearchActiveChange = {},
            onSearchResultClick = {},
            onClearHistory = {},
            onManualPin = {},
            onGetCurrentLocation = {},
            onClear = {},
            onConfirm = {}
        )
    }
}
