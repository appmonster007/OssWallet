package com.osscabinet.ossWallet.ui.screens.transactions.entry.sheets

import android.Manifest
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
import androidx.compose.ui.tooling.preview.Preview
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
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationSelection(
    currentCoords: Pair<Double, Double>?,
    onLocationSelected: (Pair<Double, Double>?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCoords by remember { mutableStateOf(currentCoords) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<SearchResult>()) }

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
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5")
                    .header("User-Agent", "OssWallet-Android-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val jsonArray = JSONArray(body)
                        val results = mutableListOf<SearchResult>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            results.add(
                                SearchResult(
                                    display = obj.getString("display_name"),
                                    lat = obj.getDouble("lat"),
                                    lon = obj.getDouble("lon")
                                )
                            )
                        }
                        searchResults = results
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    AeonFullScreenDialog(onDismissRequest = onDismiss) {
        LocationSelectionContent(
            title = "Location",
            selectedCoords = selectedCoords,
            searchQuery = searchQuery,
            searchResults = searchResults,
            onBack = onDismiss,
            onSearchQueryChange = { query ->
                searchQuery = query
                if (query.length > 2) {
                    scope.launch { performSearch(query) }
                } else {
                    searchResults = emptyList()
                }
            },
            onSearchResultClick = { result ->
                selectedCoords = result.lat to result.lon
                searchResults = emptyList()
                searchQuery = result.display
            },
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
fun LocationSelectionContent(
    title: String,
    selectedCoords: Pair<Double, Double>?,
    searchQuery: String,
    searchResults: List<SearchResult>,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchResultClick: (SearchResult) -> Unit,
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
            // Map layer
            AeonOSMMap(
                initialPoint = GeoPoint(
                    selectedCoords?.first ?: 0.0,
                    selectedCoords?.second ?: 0.0
                ),
                onLocationSelected = onManualPin
            )

            // Search Bar Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .zIndex(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search location...", style = MaterialTheme.typography.bodyLarge) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )

                        if (searchResults.isNotEmpty()) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                            ) {
                                items(searchResults) { result ->
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                result.display, 
                                                maxLines = 2,
                                                style = MaterialTheme.typography.bodyMedium
                                            ) 
                                        },
                                        modifier = Modifier.clickable { onSearchResultClick(result) },
                                        leadingContent = { 
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Rounded.Place, 
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    if (result != searchResults.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FAB Layer
            FloatingActionButton(
                onClick = onGetCurrentLocation,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .padding(bottom = 160.dp), // Increased padding to clear the higher bottom bar
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = "My Location")
            }

            // Bottom Confirmation Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 20.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (selectedCoords != null) {
                            Text(
                                text = "SELECTED COORDINATES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "%.6f,  %.6f".format(selectedCoords.first, selectedCoords.second),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Tap on map or search to select location",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCoords != null) {
                            Surface(
                                onClick = onClear,
                                modifier = Modifier.size(64.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Confirm Location",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SearchResult(
    val display: String,
    val lat: Double,
    val lon: Double
)

@Preview(showBackground = true)
@Composable
fun LocationSelectionPreview() {
    OssWalletTheme {
        LocationSelectionContent(
            title = "Location",
            selectedCoords = 37.7749 to -122.4194,
            searchQuery = "San Francisco",
            searchResults = listOf(
                SearchResult("San Francisco, CA, USA", 37.7749, -122.4194),
                SearchResult("San Francisco International Airport", 37.6213, -122.3790)
            ),
            onBack = {},
            onSearchQueryChange = {},
            onSearchResultClick = {},
            onManualPin = {},
            onGetCurrentLocation = {},
            onClear = {},
            onConfirm = {}
        )
    }
}
