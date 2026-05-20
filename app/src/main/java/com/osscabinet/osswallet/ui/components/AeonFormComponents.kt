package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import com.osscabinet.ossWallet.ui.theme.*
import com.osscabinet.ossWallet.ui.util.isDark
import com.osscabinet.ossWallet.ui.util.toColorInt
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

@Composable
fun AeonHeroHeader(
    title: String,
    subtitle: String? = null,
    iconName: String,
    accentColorHex: String,
    modifier: Modifier = Modifier,
    isLeftAligned: Boolean = false,
    onTitleChange: ((String) -> Unit)? = null,
    onIconClick: (() -> Unit)? = null
) {
    val currentAccentColor = accentColorHex.toColorInt().let { if (it != -1) Color(it) else AppColors.primary }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius),
        color = AppColors.insetSurface
    ) {
        if (isLeftAligned) {
            Row(
                modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
            ) {
                Surface(
                    modifier = Modifier
                        .size(AeonTheme.dimensions.iconSize3xl)
                        .then(if (onIconClick != null) Modifier.clickable { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onIconClick() 
                        } else Modifier),
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = currentAccentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppIconMap[iconName] ?: Icons.Rounded.Category,
                            contentDescription = null,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                            tint = if (currentAccentColor.isDark()) ColorOnDark else ColorOnLight
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (onTitleChange != null) {
                        BasicTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            textStyle = AppTextStyles.heroAmount.copy(
                                color = AppColors.contentPrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(currentAccentColor),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        "Account Name",
                                        style = AppTextStyles.heroAmount,
                                        color = AppColors.contentSecondary.copy(alpha = 0.3f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = title.ifBlank { "Untitled" },
                            style = AppTextStyles.heroAmount,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AppTextStyles.listItemTitle.copy(
                                color = AppColors.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(AeonTheme.dimensions.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)
            ) {
                Surface(
                    modifier = Modifier
                        .size(AeonTheme.dimensions.iconSize3xl)
                        .then(if (onIconClick != null) Modifier.clickable { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onIconClick() 
                        } else Modifier),
                    shape = RoundedCornerShape(AeonTheme.dimensions.componentCornerRadius),
                    color = currentAccentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppIconMap[iconName] ?: Icons.Rounded.Category,
                            contentDescription = null,
                            modifier = Modifier.size(AeonTheme.dimensions.iconSizeXl),
                            tint = if (currentAccentColor.isDark()) ColorOnDark else ColorOnLight
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (onTitleChange != null) {
                        BasicTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            textStyle = AppTextStyles.heroAmount.copy(
                                color = AppColors.contentPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(currentAccentColor),
                            decorationBox = { innerTextField ->
                                if (title.isEmpty()) {
                                    Text(
                                        "Account Name",
                                        style = AppTextStyles.heroAmount,
                                        color = AppColors.contentSecondary.copy(alpha = 0.3f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = title.ifBlank { "Untitled" },
                            style = AppTextStyles.heroAmount,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AppTextStyles.listItemTitle.copy(
                                color = AppColors.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AeonFormHeader(
    title: String,
    onSaveClick: () -> Unit,
    saveButtonEnabled: Boolean,
    accentColorHex: String,
    modifier: Modifier = Modifier,
    saveButtonText: String = "SAVE"
) {
    val currentAccentColor = accentColorHex.toColorInt().let { if (it != -1) Color(it) else AppColors.primary }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = AppTextStyles.heroAmount
        )

        Surface(
            shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
            color = if (saveButtonEnabled) currentAccentColor else AppColors.insetSurface,
            onClick = { if (saveButtonEnabled) onSaveClick() },
            enabled = saveButtonEnabled
        ) {
            Text(
                saveButtonText,
                modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingXl, vertical = AeonTheme.dimensions.spacingSm),
                style = AppTextStyles.drawerItemLabel.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (saveButtonEnabled) (if (currentAccentColor.isDark()) ColorOnDark else ColorOnLight) else AppColors.border
                )
            )
        }
    }
}

@Composable
fun AeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    prefix: String? = null,
    height: Dp = 44.dp
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(AeonTheme.dimensions.buttonCornerRadius),
        color = AppColors.insetSurface.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AeonTheme.dimensions.spacingLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = AppColors.border, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd))
                Spacer(modifier = Modifier.width(AeonTheme.dimensions.spacingSm))
            }
            if (prefix != null) {
                Text(
                    prefix,
                    style = AppTextStyles.listItemTitle.copy(
                        color = AppColors.border,
                        fontSize = 13.sp,
                        fontWeight = if (prefix == "HEX") FontWeight.Black else FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = AppTextStyles.listItemTitle.copy(
                    color = AppColors.contentPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (prefix == "HEX") FontWeight.Black else FontWeight.Normal
                ),
                cursorBrush = SolidColor(AppColors.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) Text(placeholder, color = AppColors.contentSecondary.copy(alpha = 0.6f))
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun AeonSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    brush: Brush,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = true,
    height: Dp = 48.dp
) {
    var isDragging by remember { mutableStateOf(false) }
    val thumbScale by animateFloatAsState(if (isDragging) 1.15f else 1f, label = "ThumbScale")

    Box(
        modifier = modifier
            .then(
                if (isHorizontal) Modifier.fillMaxWidth().height(height) 
                else Modifier.fillMaxHeight().width(height)
            )
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(CircleShape)
            .background(brush)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { 
                        isDragging = true
                        try {
                            awaitRelease()
                        } finally {
                            isDragging = false
                        }
                    }
                ) { offset ->
                    val progress = if (isHorizontal) (offset.x / size.width) else (1f - (offset.y / size.height))
                    val newValue = progress.coerceIn(0f, 1f) * (range.endInclusive - range.start) + range.start
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    val progress = if (isHorizontal) (change.position.x / size.width) else (1f - (change.position.y / size.height))
                    val newValue = progress.coerceIn(0f, 1f) * (range.endInclusive - range.start) + range.start
                    onValueChange(newValue)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = (value - range.start) / (range.endInclusive - range.start)
            val center = if (isHorizontal) {
                androidx.compose.ui.geometry.Offset(progress * size.width, size.height / 2)
            } else {
                androidx.compose.ui.geometry.Offset(size.width / 2, (1f - progress) * size.height)
            }

            // Thumb radius scales with track thickness: protrudes ~75% beyond half-track
            val trackHalf = if (isHorizontal) size.height / 2f else size.width / 2f
            val radius = trackHalf * 1.75f * thumbScale

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = radius + 2.dp.toPx(),
                center = center.copy(y = center.y + 1.5.dp.toPx())
            )
            // White thumb ring
            drawCircle(color = Color.White, radius = radius, center = center)
        }
    }
}

@Composable
fun HorizontalColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    brush: Brush,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppColors.contentSecondary.copy(alpha = 0.6f)
            )
        } else if (label != null) {
            Text(
                label,
                style = AppTextStyles.transactionSwipeAction.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = AppColors.contentSecondary.copy(alpha = 0.7f),
                modifier = Modifier.width(32.dp)
            )
        }
        
        AeonSlider(
            value = value,
            onValueChange = onValueChange,
            range = range,
            brush = brush,
            isHorizontal = true,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            if (range.endInclusive <= 1f) "${(value * 100).toInt()}%" else "${value.toInt()}°",
            style = AppTextStyles.transactionSwipeAction.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = AppColors.contentSecondary,
            modifier = Modifier.width(44.dp)
        )
    }
}

private fun hslToHex(h: Float, s: Float, l: Float): String {
    val colorInt = ColorUtils.HSLToColor(floatArrayOf(h, s, l))
    return String.format("#%06X", 0xFFFFFF and colorInt)
}

private fun hexToHsl(hex: String): Triple<Float, Float, Float> {
    val colorInt = hex.toColorInt()
    if (colorInt == -1) return Triple(200f, 0.7f, 0.5f)
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(colorInt, hsl)
    return Triple(hsl[0], hsl[1], hsl[2])
}

@Composable
fun ColorWheel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val hsl = remember(selectedColor) {
        val hslOut = FloatArray(3)
        ColorUtils.colorToHSL(selectedColor.toArgb(), hslOut)
        hslOut
    }

    val sheetBgColor = AppColors.sheetSurface

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hsl[0]) {
                    // Handle ring/rect gestures for drag
                    detectDragGestures(
                        onDragStart = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    ) { change, _ ->
                        val outerRadius = minOf(size.width, size.height) / 2f
                        val innerRadius = outerRadius * 0.65f
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val dist = sqrt(dx * dx + dy * dy)

                        val sqSize = innerRadius * 1.3f
                        val sqLeft = center.x - sqSize / 2f
                        val sqTop = center.y - sqSize / 2f

                        val inSqRect = change.position.x >= sqLeft && change.position.x <= sqLeft + sqSize &&
                            change.position.y >= sqTop && change.position.y <= sqTop + sqSize

                        if (inSqRect) {
                            val sat = ((change.position.x - sqLeft) / sqSize).coerceIn(0f, 1f)
                            val light = 1f - ((change.position.y - sqTop) / sqSize).coerceIn(0f, 1f)
                            val newColorInt = ColorUtils.HSLToColor(floatArrayOf(hsl[0], sat, light))
                            onColorSelected(Color(newColorInt))
                        } else if (dist in innerRadius..outerRadius) {
                            val angle = (atan2(dy, dx) * 180 / PI).toFloat().let { if (it < 0) it + 360f else it }
                            val newColorInt = ColorUtils.HSLToColor(floatArrayOf(angle, hsl[1], hsl[2]))
                            onColorSelected(Color(newColorInt))
                        }
                    }
                }
                .pointerInput(hsl[0]) {
                    detectTapGestures { offset ->
                        val outerRadius = minOf(size.width, size.height) / 2f
                        val innerRadius = outerRadius * 0.65f
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val dist = sqrt(dx * dx + dy * dy)

                        val sqSize = innerRadius * 1.3f
                        val sqLeft = center.x - sqSize / 2f
                        val sqTop = center.y - sqSize / 2f

                        val inSqRect = offset.x >= sqLeft && offset.x <= sqLeft + sqSize &&
                            offset.y >= sqTop && offset.y <= sqTop + sqSize

                        if (inSqRect) {
                            val sat = ((offset.x - sqLeft) / sqSize).coerceIn(0f, 1f)
                            val light = 1f - ((offset.y - sqTop) / sqSize).coerceIn(0f, 1f)
                            val newColorInt = ColorUtils.HSLToColor(floatArrayOf(hsl[0], sat, light))
                            onColorSelected(Color(newColorInt))
                        } else if (dist in innerRadius..outerRadius) {
                            val angle = (atan2(dy, dx) * 180 / PI).toFloat().let { if (it < 0) it + 360f else it }
                            val newColorInt = ColorUtils.HSLToColor(floatArrayOf(angle, hsl[1], hsl[2]))
                            onColorSelected(Color(newColorInt))
                        }
                    }
                }
        ) {
            val outerRadius = size.minDimension / 2f
            val innerRadius = outerRadius * 0.65f
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

            // Draw hue ring with sweep gradient
            val hueColors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
            drawCircle(
                brush = Brush.sweepGradient(hueColors, center),
                radius = outerRadius
            )
            // Cover inner with background color to create ring effect
            drawCircle(
                color = sheetBgColor,
                radius = innerRadius - 1.dp.toPx()
            )

            // Ring border
            drawCircle(
                color = Color.Black.copy(alpha = 0.12f),
                radius = outerRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.08f),
                radius = innerRadius,
                style = Stroke(width = 1.dp.toPx())
            )

            // Saturation-Lightness square inside ring
            val sqSize = innerRadius * 1.3f
            val sqLeft = center.x - sqSize / 2f
            val sqTop = center.y - sqSize / 2f
            val sqTopLeft = androidx.compose.ui.geometry.Offset(sqLeft, sqTop)
            val squareSize = androidx.compose.ui.geometry.Size(sqSize, sqSize)

            // Background: white → pure hue (saturation axis, horizontal)
            val pureHueColor = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], 1f, 0.5f)))
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.White, pureHueColor),
                    startX = sqLeft,
                    endX = sqLeft + sqSize
                ),
                topLeft = sqTopLeft,
                size = squareSize
            )
            // Overlay: transparent → black (lightness axis, vertical)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black),
                    startY = sqTop,
                    endY = sqTop + sqSize
                ),
                topLeft = sqTopLeft,
                size = squareSize
            )

            // SL crosshair handle
            val slX = sqLeft + hsl[1] * sqSize
            val slY = sqTop + (1f - hsl[2]) * sqSize
            val slHandle = androidx.compose.ui.geometry.Offset(slX, slY)

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 11.dp.toPx(),
                center = slHandle.copy(y = slHandle.y + 1.dp.toPx())
            )
            // White outer
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = slHandle)
            // Color core
            drawCircle(color = selectedColor, radius = 6.dp.toPx(), center = slHandle)
            // Black stroke
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = 10.dp.toPx(),
                center = slHandle,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Hue ring handle dot
            val hueAngleRad = (hsl[0] * PI / 180).toFloat()
            val hueHandleR = (outerRadius + innerRadius) / 2f
            val hueHandleX = center.x + hueHandleR * cos(hueAngleRad)
            val hueHandleY = center.y + hueHandleR * sin(hueAngleRad)
            val hueHandle = androidx.compose.ui.geometry.Offset(hueHandleX, hueHandleY)
            val hueHandleColor = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], 1f, 0.5f)))

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 13.dp.toPx(),
                center = hueHandle.copy(y = hueHandle.y + 1.dp.toPx())
            )
            // White outer
            drawCircle(color = Color.White, radius = 12.dp.toPx(), center = hueHandle)
            // Hue color core
            drawCircle(color = hueHandleColor, radius = 8.dp.toPx(), center = hueHandle)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedColorPickerContent(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0
) {
    var hsl by remember(initialColorHex) {
        mutableStateOf(hexToHsl(initialColorHex))
    }
    var hexInput by remember(initialColorHex) { mutableStateOf(initialColorHex) }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Tab Switcher (Segmented Control)
        Surface(
            modifier = Modifier
                .width(220.dp)
                .height(40.dp),
            shape = CircleShape,
            color = AppColors.insetSurface.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.divider.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val selectedTabX = animateDpAsState(
                    targetValue = if (pagerState.currentPage == 0) 0.dp else 110.dp,
                    label = "TabSelection"
                )

                // Animated Pill
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .offset { androidx.compose.ui.unit.IntOffset(selectedTabX.value.roundToPx(), 0) }
                        .width(102.dp)
                        .fillMaxHeight()
                        .background(AppColors.primary, CircleShape)
                        .border(1.dp, ColorOnLight.copy(alpha = 0.15f), CircleShape)
                )

                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    listOf("Sliders", "Wheel").forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                title,
                                style = AppTextStyles.transactionSwipeAction.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) (if (AppColors.primary.isDark()) ColorOnDark else ColorOnLight) else AppColors.contentSecondary
                            )
                        }
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) { page ->
            if (page == 0) {
                // HSL Sliders page — label+value row above each full-width slider
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    // HUE
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "HUE",
                                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = AppColors.contentSecondary
                            )
                            Text(
                                "${hsl.first.toInt()}°",
                                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = AppColors.contentPrimary
                            )
                        }
                        AeonSlider(
                            value = hsl.first,
                            onValueChange = { newVal ->
                                hsl = hsl.copy(first = newVal)
                                val newHex = hslToHex(hsl.first, hsl.second, hsl.third)
                                hexInput = newHex
                                onColorSelected(newHex)
                            },
                            range = 0f..360f,
                            brush = Brush.horizontalGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            height = 12.dp
                        )
                    }

                    // SATURATION
                    val greyAtL = Color(ColorUtils.HSLToColor(floatArrayOf(hsl.first, 0f, hsl.third)))
                    val vividAtL = Color(ColorUtils.HSLToColor(floatArrayOf(hsl.first, 1f, hsl.third)))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SATURATION",
                                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = AppColors.contentSecondary
                            )
                            Text(
                                "${(hsl.second * 100).toInt()}%",
                                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = AppColors.contentPrimary
                            )
                        }
                        AeonSlider(
                            value = hsl.second,
                            onValueChange = { newVal ->
                                hsl = hsl.copy(second = newVal)
                                val newHex = hslToHex(hsl.first, hsl.second, hsl.third)
                                hexInput = newHex
                                onColorSelected(newHex)
                            },
                            range = 0f..1f,
                            brush = Brush.horizontalGradient(listOf(greyAtL, vividAtL)),
                            modifier = Modifier.fillMaxWidth(),
                            height = 12.dp
                        )
                    }

                    // LIGHTNESS
                    val midHue = Color(ColorUtils.HSLToColor(floatArrayOf(hsl.first, hsl.second, 0.5f)))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "LIGHTNESS",
                                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = AppColors.contentSecondary
                            )
                            Text(
                                "${(hsl.third * 100).toInt()}%",
                                style = AppTextStyles.transactionSwipeAction.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = AppColors.contentPrimary
                            )
                        }
                        AeonSlider(
                            value = hsl.third,
                            onValueChange = { newVal ->
                                hsl = hsl.copy(third = newVal)
                                val newHex = hslToHex(hsl.first, hsl.second, hsl.third)
                                hexInput = newHex
                                onColorSelected(newHex)
                            },
                            range = 0f..1f,
                            brush = Brush.horizontalGradient(listOf(Color.Black, midHue, Color.White)),
                            modifier = Modifier.fillMaxWidth(),
                            height = 12.dp
                        )
                    }
                }
            } else {
                // Wheel page
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    val currentHslColor = hexInput.toColorInt().let {
                        if (it != -1) Color(it)
                        else Color(ColorUtils.HSLToColor(floatArrayOf(hsl.first, hsl.second, hsl.third)))
                    }
                    ColorWheel(
                        selectedColor = currentHslColor,
                        onColorSelected = { color ->
                            val newHex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                            val newHsl = hexToHsl(newHex)
                            hsl = newHsl
                            hexInput = hslToHex(newHsl.first, newHsl.second, newHsl.third)
                            onColorSelected(hexInput)
                        },
                        modifier = Modifier.size(240.dp)
                    )
                }
            }
        }

        // Preview and HEX (shared bottom row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Split preview swatch
            Box(
                modifier = Modifier
                    .size(56.dp, 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, AppColors.divider.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        val resetHsl = hexToHsl(initialColorHex)
                        hsl = resetHsl
                        hexInput = initialColorHex
                        onColorSelected(initialColorHex)
                    }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(initialColorHex.toColorInt().let { if (it != -1) Color(it) else Color.Gray })
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(ColorUtils.HSLToColor(floatArrayOf(hsl.first, hsl.second, hsl.third))))
                    )
                }
            }

            AeonTextField(
                value = hexInput,
                onValueChange = { input ->
                    hexInput = if (input.startsWith("#")) input else "#$input"
                    if (hexInput.length == 7) {
                        try {
                            val newHsl = hexToHsl(hexInput)
                            hsl = newHsl
                            onColorSelected(hexInput)
                        } catch (_: Exception) {}
                    }
                },
                placeholder = "#AABBCC",
                prefix = "HEX",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AdvancedColorPickerDialog(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = AppColors.sheetSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Color",
                        style = AppTextStyles.calculatorKey,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.contentPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Done",
                            tint = AppColors.contentPrimary
                        )
                    }
                }
                AdvancedColorPickerContent(
                    initialColorHex = initialColorHex,
                    onColorSelected = onColorSelected
                )
            }
        }
    }
}

@Composable
fun AeonColorSelection(
    accentColorHex: String,
    onColorSelected: (String) -> Unit,
    isColorLocked: Boolean = false,
    presetColors: List<String> = BrandPresetColorHexes,
    showTitle: Boolean = true
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
        if (showTitle) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                AeonFlowSectionHeader(title = "ACCENT BRAND COLOR")
                if (isColorLocked) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.border
                    )
                }
            }
        }
        
        val alpha = if (isColorLocked) 0.5f else 1f
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .alpha(alpha)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            presetColors.forEach { colorHex ->
                val isSelected = accentColorHex.equals(colorHex, ignoreCase = true)
                val color = colorHex.toColorInt().let { if (it != -1) Color(it) else Color.Gray }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) AppColors.insetSurface.copy(alpha = 0.4f)
                            else AppColors.transparent
                        )
                        .clickable(enabled = !isColorLocked) { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onColorSelected(colorHex) 
                        },
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
                                tint = if (color.isDark()) ColorOnDark else ColorOnLight,
                                modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.alpha(alpha),
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            val color = accentColorHex.toColorInt().let { if (it != -1) Color(it) else Color.DarkGray }
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color))
            AeonTextField(
                value = accentColorHex,
                onValueChange = { if (!isColorLocked && it.length <= 7) onColorSelected(if (it.startsWith("#")) it else "#$it") },
                placeholder = DefaultAccentColorHex,
                prefix = "HEX",
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { if (!isColorLocked) showColorPicker = true },
                modifier = Modifier.size(44.dp).background(AppColors.insetSurface.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Rounded.Colorize, contentDescription = "Color Picker", modifier = Modifier.size(20.dp))
            }
        }
    }

    if (showColorPicker) {
        AdvancedColorPickerDialog(
            initialColorHex = accentColorHex,
            onColorSelected = { onColorSelected(it) },
            onDismiss = { showColorPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualIdentitySection(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    accentColorHex: String,
    onColorSelected: (String) -> Unit,
    isColorLocked: Boolean = false
) {
    var iconSearchQuery by remember { mutableStateOf("") }
    val filteredIcons = if (iconSearchQuery.isBlank()) emptyList() else AppIconMap.keys.filter { it.contains(iconSearchQuery, ignoreCase = true) }.take(10)
    var showAllIcons by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
        Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
            AeonFlowSectionHeader(title = "IDENTIFIER ICON")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                AeonTextField(
                    value = iconSearchQuery,
                    onValueChange = { iconSearchQuery = it },
                    placeholder = "Search icons...",
                    leadingIcon = Icons.Rounded.Search,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showAllIcons = true },
                    modifier = Modifier.size(44.dp).background(AppColors.insetSurface.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Rounded.GridView, contentDescription = "View All Icons", modifier = Modifier.size(20.dp))
                }
            }

            if (filteredIcons.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = AeonTheme.dimensions.spacingSm),
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
                ) {
                    filteredIcons.forEach { name ->
                        val icon = AppIconMap[name]!!
                        val isSelected = selectedIconName == name
                        val accentColor = accentColorHex.toColorInt().let { if (it != -1) Color(it) else AppColors.primary }
                        Surface(
                            modifier = Modifier.size(AeonTheme.dimensions.iconSize2xl).clickable { 
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onIconSelected(name) 
                            },
                            shape = CircleShape,
                            color = if (isSelected) accentColor else AppColors.insetSurface.copy(alpha = 0.3f),
                            contentColor = if (isSelected) (if (accentColor.isDark()) ColorOnDark else ColorOnLight) else AppColors.contentSecondary
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, modifier = Modifier.size(AeonTheme.dimensions.iconSizeMd)) }
                        }
                    }
                }
            } else if (iconSearchQuery.isBlank() && selectedIconName.isNotEmpty() && AppIconMap.containsKey(selectedIconName)) {
                // Show currently selected icon if search is blank
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd),
                    modifier = Modifier.padding(top = AeonTheme.dimensions.spacingXs)
                ) {
                    val accentColor = accentColorHex.toColorInt().let { if (it != -1) Color(it) else AppColors.primary }
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = accentColor,
                        contentColor = if (accentColor.isDark()) ColorOnDark else ColorOnLight
                    ) {
                        Box(contentAlignment = Alignment.Center) { Icon(AppIconMap[selectedIconName]!!, contentDescription = null, modifier = Modifier.size(24.dp)) }
                    }
                    Text(
                        selectedIconName, 
                        style = AppTextStyles.listItemTitle.copy(
                            fontWeight = FontWeight.Medium,
                            color = AppColors.contentSecondary
                        )
                    )
                }
            }
        }

        AeonColorSelection(
            accentColorHex = accentColorHex,
            onColorSelected = onColorSelected,
            isColorLocked = isColorLocked
        )
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
                        AppIconMap.forEach { (name, icon) ->
                            val isSelected = selectedIconName == name
                            val accentColor = accentColorHex.toColorInt().let { if (it != -1) Color(it) else AppColors.primary }
                            Surface(
                                modifier = Modifier.size(48.dp).clickable { 
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onIconSelected(name)
                                    showAllIcons = false
                                },
                                shape = CircleShape,
                                color = if (isSelected) accentColor else AppColors.insetSurface.copy(alpha = 0.3f),
                                contentColor = if (isSelected) (if (accentColor.isDark()) ColorOnDark else ColorOnLight) else AppColors.contentSecondary
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
}



@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AdvancedColorPickerSlidersPreview() {
    OssWalletTheme {
        Surface(color = AppColors.sheetSurface) {
            AdvancedColorPickerContent(
                initialColorHex = "#2196F3",
                onColorSelected = {},
                modifier = Modifier.padding(24.dp),
                initialPage = 0
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AdvancedColorPickerWheelPreview() {
    OssWalletTheme {
        Surface(color = AppColors.sheetSurface) {
            AdvancedColorPickerContent(
                initialColorHex = "#2196F3",
                onColorSelected = {},
                modifier = Modifier.padding(24.dp),
                initialPage = 1
            )
        }
    }
}
