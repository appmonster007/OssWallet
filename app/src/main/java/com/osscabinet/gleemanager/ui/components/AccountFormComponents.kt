package com.osscabinet.gleemanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.util.isDark
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

@Composable
fun AeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    prefix: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (prefix != null) {
                Text(
                    prefix,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = if (prefix == "HEX") FontWeight.Black else FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (prefix == "HEX") FontWeight.Black else FontWeight.Normal
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun AccountHeroHeader(
    name: String,
    balance: String,
    currencyCode: String,
    colorHex: String,
    iconName: String,
    modifier: Modifier = Modifier
) {
    val currentAccentColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(28.dp),
                color = currentAccentColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        AccountIconMap[iconName] ?: Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (currentAccentColor.isDark()) Color.White else Color.Black
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = name.ifBlank { "Untitled Account" },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = "$currencyCode ${balance.ifBlank { "0.00" }}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrencyManagementSection(
    primaryCurrency: String,
    onPrimaryCurrencyChange: (String) -> Unit,
    allowedCurrencies: Set<String>,
    onAllowedCurrenciesChange: (Set<String>) -> Unit,
    accentColorHex: String,
    isEditMode: Boolean = false,
    currencyUsageMap: Map<String, Boolean> = emptyMap()
) {
    var searchQuery by remember { mutableStateOf("") }
    val commonCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "INR", "SGD", "HKD", "CNY", "BRL", "IDR", "MYR", "THB", "PHP", "VND", "KRW", "RUB", "TRY", "ZAR", "MXN", "ILS")
    var isSelectionMenuExpanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(allowedCurrencies) {
        if (allowedCurrencies.isEmpty()) {
            onPrimaryCurrencyChange("")
        } else if (allowedCurrencies.size == 1) {
            onPrimaryCurrencyChange(allowedCurrencies.first())
        } else if (primaryCurrency.isNotEmpty() && !allowedCurrencies.contains(primaryCurrency)) {
            onPrimaryCurrencyChange(allowedCurrencies.first())
        }
    }

    val filteredCurrencies = if (searchQuery.isBlank()) {
        commonCurrencies
    } else {
        commonCurrencies.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Currency Management", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            
            // Search Bar with Multi-select toggle
            Surface(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) Text("Search ISO codes...", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            innerTextField()
                        }
                    )
                    Box {
                        IconButton(onClick = { isSelectionMenuExpanded = true }) {
                            Icon(Icons.Rounded.FilterList, contentDescription = "Selection List", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = isSelectionMenuExpanded,
                            onDismissRequest = { isSelectionMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            commonCurrencies.forEach { code ->
                                val isSelected = allowedCurrencies.contains(code)
                                val isPrimary = primaryCurrency == code
                                val hasTransactions = currencyUsageMap[code] ?: false
                                val canToggle = if (isEditMode) !isSelected || (!isPrimary && !hasTransactions) else true

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = null,
                                                enabled = canToggle
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(code)
                                        }
                                    },
                                    onClick = {
                                        if (canToggle) {
                                            if (isSelected) {
                                                if (isPrimary) onPrimaryCurrencyChange("")
                                                onAllowedCurrenciesChange(allowedCurrencies - code)
                                            } else {
                                                onAllowedCurrenciesChange(allowedCurrencies + code)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredCurrencies.forEach { code ->
                    val isSelected = allowedCurrencies.contains(code)
                    val isPrimary = primaryCurrency == code
                    val hasTransactions = currencyUsageMap[code] ?: false
                    
                    val canToggle = if (isEditMode) {
                        !isSelected || (!isPrimary && !hasTransactions)
                    } else {
                        true
                    }

                    val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }
                    
                    Surface(
                        modifier = Modifier
                            .alpha(if (canToggle) 1f else 0.5f)
                            .clip(CircleShape)
                            .clickable(enabled = canToggle) {
                                if (isSelected) {
                                    if (isPrimary) onPrimaryCurrencyChange("")
                                    onAllowedCurrenciesChange(allowedCurrencies - code)
                                } else {
                                    onAllowedCurrenciesChange(allowedCurrencies + code)
                                }
                            },
                        shape = CircleShape,
                        color = when {
                            isPrimary -> accentColor
                            isSelected -> accentColor.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        contentColor = when {
                            isPrimary -> if (accentColor.isDark()) Color.White else Color.Black
                            isSelected -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(code, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            if (isPrimary) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.Star, contentDescription = "Primary", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Default Currency Selection (Moved after allowed currencies)
            if (allowedCurrencies.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isInvalid = primaryCurrency.isBlank()
                    Text(
                        "Primary Currency" + if (isInvalid) " (Required)" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    var isExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(
                                    width = if (isInvalid) 2.dp else 0.dp,
                                    color = if (isInvalid) MaterialTheme.colorScheme.error else Color.Transparent,
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .clickable { isExpanded = true },
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isInvalid) 2.dp else 0.dp
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    primaryCurrency.ifEmpty { "Select Primary Currency" },
                                    color = if (primaryCurrency.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (primaryCurrency.isEmpty()) FontWeight.Normal else FontWeight.Bold
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = if (isInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colorScheme.surface)
                        ) {
                            allowedCurrencies.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code, fontWeight = if (code == primaryCurrency) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onPrimaryCurrencyChange(code)
                                        isExpanded = false
                                    },
                                    leadingIcon = {
                                        if (code == primaryCurrency) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (allowedCurrencies.size == 1) {
                // If only one, it's already primary, but we can show a confirmation label
                Text(
                    "Primary Currency: $primaryCurrency",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (isEditMode) {
                Text("Note: Currencies with existing transactions cannot be removed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun VerticalColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(brush)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newValue = (1f - (offset.y / size.height)).coerceIn(0f, 1f) * (range.endInclusive - range.start) + range.start
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newValue = (1f - (change.position.y / size.height)).coerceIn(0f, 1f) * (range.endInclusive - range.start) + range.start
                    onValueChange(newValue)
                }
            }
    ) {
        val thumbY = (1f - (value - range.start) / (range.endInclusive - range.start))
        val y = thumbY * size.height
        
        drawCircle(
            color = Color.White,
            radius = 12.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(size.width / 2, y)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = 12.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(size.width / 2, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun ColorWheel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val hsv = remember(selectedColor) {
            val hsvOut = FloatArray(3)
            android.graphics.Color.colorToHSV(selectedColor.toArgb(), hsvOut)
            hsvOut
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hsv[2]) {
                    detectDragGestures { change, _ ->
                        val radius = size.width / 2f
                        val center = androidx.compose.ui.geometry.Offset(radius, radius)
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist <= radius) {
                            val angle = (atan2(dy, dx) * 180 / PI).toFloat().let { if (it < 0) it + 360f else it }
                            val saturation = (dist / radius).coerceIn(0f, 1f)
                            onColorSelected(Color.hsv(angle, saturation, hsv[2]))
                        }
                    }
                }
                .pointerInput(hsv[2]) {
                    detectTapGestures { offset ->
                        val radius = size.width / 2f
                        val center = androidx.compose.ui.geometry.Offset(radius, radius)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist <= radius) {
                            val angle = (atan2(dy, dx) * 180 / PI).toFloat().let { if (it < 0) it + 360f else it }
                            val saturation = (dist / radius).coerceIn(0f, 1f)
                            onColorSelected(Color.hsv(angle, saturation, hsv[2]))
                        }
                    }
                }
        ) {
            val radius = size.minDimension / 2f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            
            val colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
            
            // Full Spectrum Disc
            drawCircle(
                brush = Brush.sweepGradient(colors, center),
                radius = radius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius
            )

            // Handle Position
            val angle = (hsv[0] * PI / 180).toFloat()
            val r = hsv[1] * radius
            val handleX = center.x + r * cos(angle)
            val handleY = center.y + r * sin(angle)

            // Glass Handle (3D Effect)
            drawCircle(
                color = Color.White,
                radius = 18.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(handleX, handleY)
            )
            drawCircle(
                color = selectedColor,
                radius = 14.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(handleX, handleY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 14.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(handleX, handleY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedColorPickerDialog(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hsv by remember {
        val hsvOut = FloatArray(3)
        try {
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(initialColorHex), hsvOut)
        } catch (e: Exception) {
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor("#004D5F"), hsvOut)
        }
        mutableStateOf(Triple(hsvOut[0], hsvOut[1], hsvOut[2]))
    }

    val pagerState = rememberPagerState(pageCount = { 2 })

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        title = { Text("Color Selection") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                ) { page ->
                    if (page == 0) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Hue Slider
                            VerticalColorSlider(
                                value = hsv.first,
                                onValueChange = { 
                                    hsv = hsv.copy(first = it)
                                    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                    onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                                },
                                range = 0f..360f,
                                brush = Brush.verticalGradient(
                                    listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red).reversed()
                                )
                            )
                                // Saturation Slider
                            VerticalColorSlider(
                                value = hsv.second,
                                onValueChange = { 
                                    hsv = hsv.copy(second = it)
                                    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                    onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                                },
                                range = 0f..1f,
                                brush = Brush.verticalGradient(
                                    listOf(Color.hsv(hsv.first, 1f, hsv.third), Color.hsv(hsv.first, 0f, hsv.third))
                                )
                            )
                            // Brightness Slider
                            VerticalColorSlider(
                                value = hsv.third,
                                onValueChange = { 
                                    hsv = hsv.copy(third = it)
                                    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                    onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                                },
                                range = 0f..1f,
                                brush = Brush.verticalGradient(
                                    listOf(Color.hsv(hsv.first, hsv.second, 1f), Color.Black)
                                )
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ColorWheel(
                                selectedColor = Color.hsv(hsv.first, hsv.second, hsv.third),
                                onColorSelected = { color ->
                                    val newHsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), newHsv)
                                    hsv = Triple(newHsv[0], newHsv[1], hsv.third) // Keep brightness
                                    val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                    onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                                },
                                modifier = Modifier.size(220.dp)
                            )
                            
                            // Brightness Slider for Wheel Page
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                Slider(
                                    value = hsv.third,
                                    onValueChange = { 
                                        hsv = hsv.copy(third = it)
                                        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                        onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.hsv(hsv.first, hsv.second, 1f),
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                Text(
                                    "${(hsv.third * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }
                    }
                }

                // Page Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    repeat(2) { index ->
                        val color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Preview and HEX
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val currentColor = Color.hsv(hsv.first, hsv.second, hsv.third)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentColor)
                    )
                    AeonTextField(
                        value = initialColorHex,
                        onValueChange = { 
                            if (it.length <= 7) {
                                val newHex = if (it.startsWith("#")) it else "#$it"
                                try {
                                    val hsvOut = FloatArray(3)
                                    android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(newHex), hsvOut)
                                    hsv = Triple(hsvOut[0], hsvOut[1], hsvOut[2])
                                    onColorSelected(newHex)
                                } catch (e: Exception) {
                                    onColorSelected(newHex)
                                }
                            }
                        },
                        placeholder = "#004D5F",
                        prefix = "HEX",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VisualIdentitySection(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    accentColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val presetColors = listOf("#004D5F", "#1A237E", "#1B5E20", "#B71C1C", "#4A148C", "#E65100", "#006064", "#212121", "#795548", "#607D8B")
    var iconSearchQuery by remember { mutableStateOf("") }
    val filteredIcons = if (iconSearchQuery.isBlank()) emptyList() else AccountIconMap.keys.filter { it.contains(iconSearchQuery, ignoreCase = true) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showAllIcons by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(40.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Text("Visual Identity", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Identifier Icon", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AeonTextField(
                        value = iconSearchQuery,
                        onValueChange = { iconSearchQuery = it },
                        placeholder = "Search 100+ icons...",
                        leadingIcon = Icons.Rounded.Search,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showAllIcons = true },
                        modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.GridView, contentDescription = "View All Icons")
                    }
                }

                if (filteredIcons.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filteredIcons.forEach { name ->
                            val icon = AccountIconMap[name]!!
                            val isSelected = selectedIconName == name
                            val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }
                            Surface(
                                modifier = Modifier.size(48.dp).clickable { onIconSelected(name) },
                                shape = CircleShape,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                contentColor = if (isSelected) (if (accentColor.isDark()) Color.White else Color.Black) else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
                            }
                        }
                    }
                } else if (iconSearchQuery.isBlank() && selectedIconName.isNotEmpty() && AccountIconMap.containsKey(selectedIconName)) {
                    // Show currently selected icon if search is blank
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = accentColor,
                            contentColor = if (accentColor.isDark()) Color.White else Color.Black
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(AccountIconMap[selectedIconName]!!, contentDescription = null, modifier = Modifier.size(24.dp)) }
                        }
                        Text(selectedIconName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Accent Brand Color", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presetColors.forEach { colorHex ->
                        val isSelected = accentColorHex.equals(colorHex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .clickable { onColorSelected(colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (color.isDark()) Color.White else Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val color = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color.DarkGray }
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color))
                    AeonTextField(
                        value = accentColorHex,
                        onValueChange = { if (it.length <= 7) onColorSelected(if (it.startsWith("#")) it else "#$it") },
                        placeholder = "#004D5F",
                        prefix = "HEX",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showColorPicker = true },
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Colorize, contentDescription = "Color Picker")
                    }
                }
            }
        }
    }

    if (showAllIcons) {
        AlertDialog(
            onDismissRequest = { showAllIcons = false },
            title = { Text("Select Icon") },
            text = {
                Box(modifier = Modifier.height(400.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccountIconMap.forEach { (name, icon) ->
                            val isSelected = selectedIconName == name
                            val accentColor = try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (e: Exception) { Color(0xFF004D5F) }
                            Surface(
                                modifier = Modifier.size(48.dp).clickable { 
                                    onIconSelected(name)
                                    showAllIcons = false
                                },
                                shape = CircleShape,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                contentColor = if (isSelected) (if (accentColor.isDark()) Color.White else Color.Black) else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllIcons = false }) { Text("Close") }
            }
        )
    }

    if (showColorPicker) {
        AdvancedColorPickerDialog(
            initialColorHex = accentColorHex,
            onColorSelected = onColorSelected,
            onDismiss = { showColorPicker = false }
        )
    }
}
