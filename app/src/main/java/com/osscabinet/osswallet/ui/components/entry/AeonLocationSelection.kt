package com.osscabinet.ossWallet.ui.components.entry

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.osscabinet.ossWallet.ui.components.AeonSelectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    rememberCoroutineScope()
    var selectedCoords by remember { mutableStateOf(currentCoords) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<LocationSearchResult>()) }
    var searchHistory by remember { mutableStateOf(emptyList<LocationSearchResult>()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun getCurrentLocation() {
        if (locationPermissionState.status.isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        selectedCoords = it.latitude to it.longitude
                    }
                }
            } catch (_: SecurityException) {
            }
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
                            
                            val secondaryElements = mutableListOf<String>()
                            address?.let { addr ->
                                addr.optString("suburb").takeIf { it.isNotBlank() }?.let { secondaryElements.add(it) }
                                addr.optString("city").takeIf { it.isNotBlank() }?.let { secondaryElements.add(it) }
                                    ?: addr.optString("town").takeIf { it.isNotBlank() }?.let { secondaryElements.add(it) }
                                    ?: addr.optString("village").takeIf { it.isNotBlank() }?.let { secondaryElements.add(it) }
                                addr.optString("state").takeIf { it.isNotBlank() }?.let { secondaryElements.add(it) }
                                addr.optString("country").takeIf { it.isNotBlank() }?.let { secondaryElements.add(it) }
                            }
                            
                            var secondaryName = secondaryElements.distinct().joinToString(", ")
                            if (secondaryName.isBlank()) {
                                secondaryName = obj.getString("display_name")
                                    .replaceFirst(mainName, "")
                                    .trim().trimStart(',')
                                    .trim()
                            }

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
            } catch (e: Exception) {
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
            title = title,
            selectedCoords = selectedCoords,
            selectedAddress = selectedAddress,
            searchQuery = searchQuery,
            searchResults = searchResults,
            searchHistory = searchHistory,
            isSearching = isSearching,
            searchError = searchError,
            onBack = onDismiss,
            onSearchQueryChange = { query -> searchQuery = query },
            onSearchResultClick = { result ->
                selectedCoords = result.lat to result.lon
                searchResults = emptyList()
                searchQuery = result.display
                searchHistory = (listOf(result) + searchHistory.filter { it.display != result.display }).take(5)
            },
            onClearHistory = { searchHistory = emptyList() },
            onManualPin = { geoPoint ->
                selectedCoords = geoPoint.latitude to geoPoint.longitude
                searchQuery = ""
                searchResults = emptyList()
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

@Composable
fun AeonLocationSelectionContent(
    title: String,
    selectedCoords: Pair<Double, Double>?,
    selectedAddress: String?,
    searchQuery: String,
    searchResults: List<LocationSearchResult>,
    searchHistory: List<LocationSearchResult>,
    isSearching: Boolean,
    searchError: String?,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchResultClick: (LocationSearchResult) -> Unit,
    onClearHistory: () -> Unit,
    onManualPin: (GeoPoint) -> Unit,
    onGetCurrentLocation: () -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit
) {
    AeonSelectionScreen(
        title = title,
        onBack = onBack
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AeonOSMMap(
                initialPoint = GeoPoint(
                    selectedCoords?.first ?: 0.0,
                    selectedCoords?.second ?: 0.0
                ),
                onLocationSelected = onManualPin
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = AppColors.panelSurface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.5f))
                ) {
                    Column {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search location...", style = AppTextStyles.listItemTitle) },
                            leadingIcon = { 
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AppColors.primary
                                    )
                                } else {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = AppColors.primary)
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AppColors.transparent,
                                unfocusedContainerColor = AppColors.transparent,
                                disabledContainerColor = AppColors.transparent,
                                focusedIndicatorColor = AppColors.transparent,
                                unfocusedIndicatorColor = AppColors.transparent,
                            ),
                            singleLine = true,
                            textStyle = AppTextStyles.listItemTitle
                        )

                        val displayResults = if (searchQuery.isEmpty()) searchHistory else searchResults
                        
                        if (displayResults.isNotEmpty() || searchError != null) {
                            HorizontalDivider(thickness = 0.5.dp, color = AppColors.divider)
                            
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
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "RECENT SEARCHES",
                                        style = AppTextStyles.transactionSwipeAction.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = AppColors.primary
                                    )
                                    TextButton(
                                        onClick = onClearHistory,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Clear All", style = AppTextStyles.transactionSwipeAction)
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                            ) {
                                items(displayResults) { result ->
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                result.mainText, 
                                                maxLines = 1,
                                                style = AppTextStyles.formFieldValue
                                            ) 
                                        },
                                        supportingContent = {
                                            Text(
                                                result.secondaryText,
                                                maxLines = 1,
                                                style = AppTextStyles.caption,
                                                color = AppColors.contentSecondary
                                            )
                                        },
                                        modifier = Modifier.clickable { onSearchResultClick(result) },
                                        leadingContent = { 
                                            Surface(
                                                shape = CircleShape,
                                                color = if (searchQuery.isEmpty()) 
                                                    AppColors.secondaryContainer 
                                                else 
                                                    AppColors.accentContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        if (searchQuery.isEmpty()) Icons.Rounded.History else Icons.Rounded.Place, 
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = if (searchQuery.isEmpty())
                                                            AppColors.onSecondaryContainer
                                                        else
                                                            AppColors.onAccentContainer
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onGetCurrentLocation,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .padding(bottom = if (selectedCoords != null) 210.dp else 24.dp),
                containerColor = AppColors.accentContainer,
                contentColor = AppColors.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = "My Location")
            }

            AnimatedVisibility(
                visible = selectedCoords != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.panelSurface,
                    tonalElevation = 8.dp,
                    shadowElevation = 24.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    border = BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (selectedCoords != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = AppColors.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Place,
                                            contentDescription = null,
                                            tint = AppColors.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedAddress?.split(",")?.firstOrNull() ?: "Selected Location",
                                        style = AppTextStyles.cardSectionTitle,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedAddress?.substringAfter(",")?.trim() ?: "%.6f, %.6f".format(selectedCoords.first, selectedCoords.second),
                                        style = AppTextStyles.caption,
                                        color = AppColors.contentSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedIconButton(
                                onClick = onClear,
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, AppColors.divider)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Clear", tint = AppColors.error)
                            }

                            Button(
                                onClick = onConfirm,
                                enabled = selectedCoords != null,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Confirm Location", style = AppTextStyles.managementItemTitle)
                            }
                        }
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
