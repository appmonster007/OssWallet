package com.osscabinet.ossWallet.ui.components.navigation

import com.osscabinet.ossWallet.ui.theme.GlassElement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorScrimDark
import com.osscabinet.ossWallet.ui.theme.ColorScrimPill
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalAeonDimensions
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import com.osscabinet.ossWallet.ui.theme.LocalThemeMode
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import com.osscabinet.ossWallet.ui.theme.copyStyle

/**
 * NavigationTab - Data class for navigation tab
 */
data class NavigationTab(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
    val contentDescription: String = label
)

@Composable
fun UnifiedPillNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavigationTab>,
    modifier: Modifier = Modifier,
    fabPaddingBottom: Dp = 16.dp,
    showFab: Boolean = true,
    isFabExpanded: Boolean = false,
    onFabToggle: (Boolean) -> Unit = {},
    fabIcon: ImageVector = Icons.Rounded.Add,
    onFabClick: () -> Unit = {},
    onFabLongPress: (() -> Unit)? = null,
    fabMenu: @Composable (ColumnScope.() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Scrim backdrop when FAB is expanded
        AnimatedVisibility(
            visible = isFabExpanded,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorScrimDark)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onFabToggle(false) }
                    )
            )
        }

        // Bottom Navigation Assembly
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = fabPaddingBottom)
                .padding(horizontal = 24.dp)
                .zIndex(2f)
        ) {
            // Floating Menu Actions
            if (showFab && fabMenu != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 64.dp)
                        .wrapContentWidth(align = Alignment.End, unbounded = true)
                ) {
                    fabMenu()
                }
            }

            // Bottom Bar Container (Pill + FAB)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
            ) {
                // Centered Pill Navigation
                if (tabs.isNotEmpty()) {
                    PillNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        tabs = tabs,
                        isDimmed = isFabExpanded,
                        onDimClick = { onFabToggle(false) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Primary FAB
                if (showFab) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isFabExpanded) 45f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "FabRotation"
                    )

                    val haptic = LocalHapticFeedback.current
                    if (onFabLongPress != null) {
                        // Quick-add mode: tap → quick sheet, long press → full entry screen.
                        // We build the FAB appearance manually so combinedClickable is the only
                        // gesture handler (FloatingActionButton's internal Surface would consume
                        // the click before combinedClickable could see it).
                        @OptIn(ExperimentalFoundationApi::class)
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(48.dp)
                                .graphicsLayer { rotationZ = rotation }
                                .border(1.dp, AppColors.onPrimary.copy(alpha = 0.20f), CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onFabClick()
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onFabLongPress()
                                    }
                                ),
                            shape = CircleShape,
                            color = AppColors.primary,
                            contentColor = AppColors.onPrimary,
                            shadowElevation = 6.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = fabIcon,
                                    contentDescription = "Add New",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        // Normal mode: FAB menu toggle (or direct navigate if no menu).
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (fabMenu != null) onFabToggle(!isFabExpanded) else onFabClick()
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(48.dp)
                                .graphicsLayer { rotationZ = rotation }
                                .border(1.dp, AppColors.onPrimary.copy(alpha = 0.20f), CircleShape),
                            containerColor = AppColors.primary,
                            contentColor = AppColors.onPrimary,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Icon(
                                imageVector = fabIcon,
                                contentDescription = if (isFabExpanded) "Close Menu" else "Add New",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PillNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavigationTab>,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    onDimClick: () -> Unit = {}
) {
    val dimensions = LocalAeonDimensions.current

    // Grid constants derived from the Aeon Design System
    val spacing = dimensions.spacingSm / 2 // 8.dp (The base grid unit)
    val itemWidth = AeonSize.D56.value // Standard touch target width
    val barHeight = AeonSize.D54.value // Matches Material BottomAppBar height
    val indicatorHeight =
        barHeight - (2.times(spacing)) // Leaves exactly 8dp (spacingSm) vertical padding

    // Total width = (ItemWidth * Tabs) + (Spacing * (Tabs + 1))
    val barWidth = (itemWidth * tabs.size) + (spacing * (tabs.size + 1))

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            element = GlassElement.NAV_PILL,
            modifier = Modifier
                .width(barWidth)
                .height(barHeight),
            shape = RoundedCornerShape(barHeight / 2),
            hazeLayer = HazeLayer.Overlay,
            hazeStyle = GlassTokens.pillStyle(),
            glassVariant = GlassVariant.Pill,
            fallbackColor = AppColors.componentOnCard,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Sliding Selection Indicator
                val indicatorOffset by animateDpAsState(
                    targetValue = spacing + (itemWidth + spacing) * selectedTab,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "indicatorOffset"
                )

                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                        .width(itemWidth)
                        .height(indicatorHeight)
                        .align(Alignment.CenterStart)
                        .background(
                            color = AppColors.primaryContainer,
                            shape = RoundedCornerShape(indicatorHeight / 2)
                        )
                )

                // Navigation Items
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        PillNavigationItem(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            icon = tab.icon,
                            selectedIcon = tab.selectedIcon,
                            contentDescription = tab.contentDescription,
                            modifier = Modifier
                                .width(itemWidth)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        // Pill-specific dimming scrim
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isDimmed) 0.45f else 0f,
            animationSpec = tween(300),
            label = "pillScrimAlpha"
        )

        if (isDimmed || scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(56.dp)
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(ColorScrimPill, CircleShape)
                    .then(
                        if (isDimmed) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDimClick
                            )
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
fun PillNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    selectedIcon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "itemScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = contentDescription,
            tint = if (selected)
                AppColors.onSelectionContainer
            else
                AppColors.contentSecondary.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun TransactionFabMenu(
    isExpanded: Boolean,
    templates: List<Template>,
    categories: List<Category> = emptyList(),
    onAddClick: (EntryMode?, Long?) -> Unit,
    onAddAccountClick: (() -> Unit)? = null
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val displayTemplates = remember(templates) {
        val starred = templates.filter { it.isStarred }
        if (starred.isNotEmpty()) starred.take(5) else templates.take(5)
    }

    displayTemplates.forEachIndexed { index, template ->
        val category = categoryMap[template.categoryId]
        val icon = getIconByName(category?.iconName)
        val iconColor = category?.colorHex?.let { Color(it) }
        FabMenuItem(
            visible = isExpanded,
            label = template.name,
            icon = icon,
            index = index,
            iconColor = iconColor,
            onClick = { onAddClick(template.entryMode, template.id) }
        )
    }

    val baseIndex = displayTemplates.size

    if (onAddAccountClick != null) {
        FabMenuItem(
            visible = isExpanded,
            label = "Add Account",
            icon = Icons.Rounded.AccountBalance,
            index = baseIndex,
            onClick = onAddAccountClick
        )
    }

    FabMenuItem(
        visible = isExpanded,
        label = "Transfer",
        icon = Icons.Rounded.SyncAlt,
        index = if (onAddAccountClick != null) baseIndex + 1 else baseIndex,
        onClick = { onAddClick(EntryMode.TRANSFER, null) }
    )

    FabMenuItem(
        visible = isExpanded,
        label = "New Transaction",
        icon = Icons.Rounded.Add,
        index = if (onAddAccountClick != null) baseIndex + 2 else baseIndex + 1,
        onClick = { onAddClick(EntryMode.EXPENSE, null) }
    )
}

@Composable
fun FabMenuItem(
    visible: Boolean,
    label: String,
    icon: ImageVector,
    index: Int,
    iconColor: Color? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "menuItemScale"
    )

    val staggerDelay = index * 40

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) +
                fadeIn(animationSpec = tween(200, delayMillis = staggerDelay)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(200, delayMillis = staggerDelay)
                ),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.wrapContentSize(unbounded = true)
        ) {
            // Label Glass/Solid Surface
            GlassSurface(
                element = GlassElement.NAV_PILL,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple()
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                shape = RoundedCornerShape(16.dp),
                hazeLayer = HazeLayer.Overlay,
                hazeStyle = GlassTokens.popupStyle(),
                glassVariant = GlassVariant.Popup,
                fallbackColor = AppColors.cardSurfaceProminent,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = AppTextStyles.drawerItemLabel,
                    color = AppColors.contentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val glassEnabled = LocalGlassmorphismEnabled.current
            val themeMode = LocalThemeMode.current

            // Resolve Icon Tint and Fallback Background based on mode
            val (resolvedIconTint, glassFallback) = if (iconColor != null) {
                if (glassEnabled) {
                    // Glass Mode: Action color icon on translucent glass
                    val fallback = when {
                        themeMode.isAmoled -> iconColor.copy(alpha = 0.18f)
                        themeMode.isDarkTheme -> iconColor.copy(alpha = 0.25f)
                        else -> iconColor.copy(alpha = 0.20f)
                    }
                    Pair(iconColor, fallback)
                } else {
                    // Non-Glass Mode: High contrast white icon on solid action color
                    Pair(Color.White, iconColor)
                }
            } else {
                // Default colors (no category color)
                if (glassEnabled) {
                    Pair(AppColors.onSecondaryContainer, AppColors.secondaryContainer)
                } else {
                    Pair(AppColors.onPrimary, AppColors.primary)
                }
            }

            // Icon Glass/Solid Surface
            GlassSurface(
                element = GlassElement.NAV_PILL,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = resolvedIconTint)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                shape = CircleShape,
                hazeLayer = HazeLayer.Overlay,
                hazeStyle = if (glassEnabled && iconColor != null) {
                    GlassTokens.pillStyle().copyStyle(
                        bgColor = iconColor,
                        alpha = if (themeMode.isDarkTheme) 0.15f else 0.25f
                    )
                } else {
                    GlassTokens.pillStyle()
                },
                glassVariant = GlassVariant.Pill,
                fallbackColor = glassFallback,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = resolvedIconTint
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UnifiedPillNavigationPreview() {
    val tabs = listOf(
        NavigationTab(Icons.Rounded.Dashboard, Icons.Rounded.Dashboard, "Dashboard"),
        NavigationTab(Icons.Rounded.History, Icons.Rounded.History, "History"),
        NavigationTab(Icons.Rounded.AccountBalance, Icons.Rounded.AccountBalance, "Accounts")
    )
    var selectedTab by remember { mutableIntStateOf(2) }
    var isExpanded by remember { mutableStateOf(false) }

    OssWalletTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
        ) {
            UnifiedPillNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                tabs = tabs,
                showFab = true,
                isFabExpanded = isExpanded,
                onFabToggle = { isExpanded = it },
                fabMenu = {
                    FabMenuItem(
                        visible = isExpanded,
                        label = "Add Template",
                        icon = Icons.Rounded.Add,
                        index = 0,
                        onClick = { isExpanded = false }
                    )
                    FabMenuItem(
                        visible = isExpanded,
                        label = "Transfer",
                        icon = Icons.Rounded.SyncAlt,
                        index = 1,
                        onClick = { isExpanded = false }
                    )
                }
            )
        }
    }
}
