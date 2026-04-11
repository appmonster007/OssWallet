package com.osscabinet.gleemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.gleemanager.ui.util.isDark

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
                Text(prefix, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.width(8.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    innerTextField()
                }
            )
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
    
    val filteredCurrencies = if (searchQuery.isBlank()) {
        commonCurrencies
    } else {
        commonCurrencies.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(48.dp),
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
                                                if (!isPrimary) onAllowedCurrenciesChange(allowedCurrencies - code)
                                            } else {
                                                onAllowedCurrenciesChange(allowedCurrencies + code)
                                                if (primaryCurrency.isBlank()) onPrimaryCurrencyChange(code)
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
                                    if (!isPrimary) onAllowedCurrenciesChange(allowedCurrencies - code)
                                } else {
                                    onAllowedCurrenciesChange(allowedCurrencies + code)
                                    if (primaryCurrency.isBlank()) onPrimaryCurrencyChange(code)
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
                    Text("Default Currency", style = MaterialTheme.typography.titleSmall)
                    var isExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(56.dp).clickable { isExpanded = true },
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(primaryCurrency.ifEmpty { "Select Default" })
                                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                            allowedCurrencies.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code) },
                                    onClick = {
                                        onPrimaryCurrencyChange(code)
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isEditMode) {
                Text("Note: Currencies with existing transactions cannot be removed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VisualIdentitySection(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    accentColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val iconMap = mapOf(
        "AccountBalance" to Icons.Rounded.AccountBalance,
        "Wallet" to Icons.Rounded.Wallet,
        "RocketLaunch" to Icons.Rounded.RocketLaunch,
        "ShowChart" to Icons.AutoMirrored.Rounded.ShowChart,
        "Payments" to Icons.Rounded.Payments,
        "Savings" to Icons.Rounded.Savings,
        "Paid" to Icons.Rounded.Paid,
        "AccountBalanceWallet" to Icons.Rounded.AccountBalanceWallet,
        "CreditCard" to Icons.Rounded.CreditCard,
        "MonetizationOn" to Icons.Rounded.MonetizationOn,
        "Analytics" to Icons.Rounded.Analytics,
        "Foundation" to Icons.Rounded.Foundation,
        "Stars" to Icons.Rounded.Stars,
        "Diamond" to Icons.Rounded.Diamond,
        "AccountTree" to Icons.Rounded.AccountTree,
        "AutoGraph" to Icons.Rounded.AutoGraph,
        "Home" to Icons.Rounded.Home,
        "Work" to Icons.Rounded.Work,
        "School" to Icons.Rounded.School,
        "ShoppingBag" to Icons.Rounded.ShoppingBag,
        "ShoppingCart" to Icons.Rounded.ShoppingCart,
        "Restaurant" to Icons.Rounded.Restaurant,
        "Flight" to Icons.Rounded.Flight,
        "DirectionsCar" to Icons.Rounded.DirectionsCar,
        "DirectionsBus" to Icons.Rounded.DirectionsBus,
        "DirectionsBike" to Icons.Rounded.DirectionsBike,
        "Hotel" to Icons.Rounded.Hotel,
        "LocalPharmacy" to Icons.Rounded.LocalPharmacy,
        "LocalHospital" to Icons.Rounded.LocalHospital,
        "FitnessCenter" to Icons.Rounded.FitnessCenter,
        "Fastfood" to Icons.Rounded.Fastfood,
        "Coffee" to Icons.Rounded.Coffee,
        "LocalDrink" to Icons.Rounded.LocalDrink,
        "Storefront" to Icons.Rounded.Storefront,
        "Build" to Icons.Rounded.Build,
        "Settings" to Icons.Rounded.Settings,
        "Lock" to Icons.Rounded.Lock,
        "Key" to Icons.Rounded.Key,
        "VpnKey" to Icons.Rounded.VpnKey,
        "Face" to Icons.Rounded.Face,
        "Groups" to Icons.Rounded.Groups,
        "Person" to Icons.Rounded.Person,
        "Pets" to Icons.Rounded.Pets,
        "Favorite" to Icons.Rounded.Favorite,
        "EmojiEvents" to Icons.Rounded.EmojiEvents,
        "SportsBasketball" to Icons.Rounded.SportsBasketball,
        "SportsSoccer" to Icons.Rounded.SportsSoccer,
        "SportsEsports" to Icons.Rounded.SportsEsports,
        "Gamepad" to Icons.Rounded.Gamepad,
        "Headphones" to Icons.Rounded.Headphones,
        "MusicNote" to Icons.Rounded.MusicNote,
        "Movie" to Icons.Rounded.Movie,
        "CameraAlt" to Icons.Rounded.CameraAlt,
        "PhotoCamera" to Icons.Rounded.PhotoCamera,
        "Smartphone" to Icons.Rounded.Smartphone,
        "Laptop" to Icons.Rounded.Laptop,
        "DesktopWindows" to Icons.Rounded.DesktopWindows,
        "Print" to Icons.Rounded.Print,
        "Cloud" to Icons.Rounded.Cloud,
        "Email" to Icons.Rounded.Email,
        "Phone" to Icons.Rounded.Phone,
        "Chat" to Icons.Rounded.Chat,
        "Forum" to Icons.Rounded.Forum,
        "Public" to Icons.Rounded.Public,
        "Language" to Icons.Rounded.Language,
        "Map" to Icons.Rounded.Map,
        "Place" to Icons.Rounded.Place,
        "Explore" to Icons.Rounded.Explore,
        "Terrain" to Icons.Rounded.Terrain,
        "Nature" to Icons.Rounded.Nature,
        "LocalFlorist" to Icons.Rounded.LocalFlorist,
        "Park" to Icons.Rounded.Park,
        "Lightbulb" to Icons.Rounded.Lightbulb,
        "FlashOn" to Icons.Rounded.FlashOn,
        "Eco" to Icons.Rounded.Eco,
        "WaterDrop" to Icons.Rounded.WaterDrop,
        "ElectricBolt" to Icons.Rounded.ElectricBolt,
        "Agriculture" to Icons.Rounded.Agriculture,
        "Construction" to Icons.Rounded.Construction,
        "Engineering" to Icons.Rounded.Engineering,
        "Gavel" to Icons.Rounded.Gavel,
        "HistoryEdu" to Icons.Rounded.HistoryEdu,
        "MenuBook" to Icons.Rounded.MenuBook,
        "Book" to Icons.Rounded.Book,
        "Receipt" to Icons.Rounded.Receipt,
        "ReceiptLong" to Icons.AutoMirrored.Rounded.ReceiptLong,
        "Inventory" to Icons.Rounded.Inventory,
        "Archive" to Icons.Rounded.Archive,
        "PushPin" to Icons.Rounded.PushPin,
        "Flag" to Icons.Rounded.Flag,
        "Terminal" to Icons.Rounded.Terminal,
        "Code" to Icons.Rounded.Code,
        "BugReport" to Icons.Rounded.BugReport,
        "Security" to Icons.Rounded.Security,
        "Shield" to Icons.Rounded.Shield,
        "Policy" to Icons.Rounded.Policy,
        "ListAlt" to Icons.AutoMirrored.Rounded.ListAlt,
        "TaskAlt" to Icons.Rounded.TaskAlt,
        "CheckCircle" to Icons.Rounded.CheckCircle,
        "Cancel" to Icons.Rounded.Cancel,
        "Error" to Icons.Rounded.Error,
        "Warning" to Icons.Rounded.Warning,
        "Info" to Icons.Rounded.Info,
        "Help" to Icons.Rounded.Help,
        "Add" to Icons.Rounded.Add,
        "Remove" to Icons.Rounded.Remove,
        "Check" to Icons.Rounded.Check,
        "Close" to Icons.Rounded.Close,
        "Search" to Icons.Rounded.Search,
        "Refresh" to Icons.Rounded.Refresh,
        "Sync" to Icons.Rounded.Sync,
        "Timer" to Icons.Rounded.Timer,
        "Schedule" to Icons.Rounded.Schedule,
        "Today" to Icons.Rounded.Today,
        "Event" to Icons.Rounded.Event,
        "CalendarMonth" to Icons.Rounded.CalendarMonth
    )
    val presetColors = listOf("#004D5F", "#1A237E", "#1B5E20", "#B71C1C", "#4A148C", "#E65100", "#006064", "#212121", "#795548", "#607D8B")
    var iconSearchQuery by remember { mutableStateOf("") }
    val filteredIcons = if (iconSearchQuery.isBlank()) iconMap.keys.toList() else iconMap.keys.filter { it.contains(iconSearchQuery, ignoreCase = true) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showAllIcons by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
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
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredIcons.forEach { name ->
                    val icon = iconMap[name]!!
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
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Accent Brand Color", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presetColors.forEach { colorHex ->
                    val isSelected = accentColorHex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(colorHex))).clickable { onColorSelected(colorHex) }.border(width = if (isSelected) 2.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Rounded.Check, contentDescription = null, tint = if (Color(android.graphics.Color.parseColor(colorHex)).isDark()) Color.White else Color.Black, modifier = Modifier.size(20.dp))
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
                        iconMap.forEach { (name, icon) ->
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
        var hsv by remember { 
            val hsvOut = FloatArray(3)
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(accentColorHex), hsvOut)
            mutableStateOf(Triple(hsvOut[0], hsvOut[1], hsvOut[2]))
        }

        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Custom Color Picker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Current Color Preview
                    val currentColor = Color.hsv(hsv.first, hsv.second, hsv.third)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(currentColor)
                    )

                    // Hue Slider
                    Column {
                        Text("Hue: ${hsv.first.toInt()}°", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = hsv.first,
                            onValueChange = { 
                                hsv = hsv.copy(first = it)
                                val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(thumbColor = currentColor)
                        )
                    }

                    // Saturation Slider
                    Column {
                        Text("Saturation: ${(hsv.second * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = hsv.second,
                            onValueChange = { 
                                hsv = hsv.copy(second = it)
                                val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = currentColor)
                        )
                    }

                    // Value/Brightness Slider
                    Column {
                        Text("Brightness: ${(hsv.third * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = hsv.third,
                            onValueChange = { 
                                hsv = hsv.copy(third = it)
                                val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, hsv.third))
                                onColorSelected(String.format("#%06X", 0xFFFFFF and colorInt))
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = currentColor)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("Done") }
            }
        )
    }
}
