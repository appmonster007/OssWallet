package com.osscabinet.ossWallet.ui.components.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.model.TransactionType
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.components.getIconByName
import com.osscabinet.ossWallet.ui.theme.*

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
                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (fabMenu != null) onFabToggle(!isFabExpanded) else onFabClick()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(48.dp)
                            .graphicsLayer { rotationZ = rotation },
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

@Composable
fun PillNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<NavigationTab>,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    onDimClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val dimensions = LocalAeonDimensions.current
    
    // Grid constants derived from the Aeon Design System
    val spacing = dimensions.spacingSm // 8.dp (The base grid unit)
    val itemWidth = AeonSize.D56.value // Standard touch target width
    val barHeight = AeonSize.D56.value // Matches Material BottomAppBar height
    val indicatorHeight = AeonSize.D40.value // Leaves exactly 8dp (spacingSm) vertical padding
    
    // Total width = (ItemWidth * Tabs) + (Spacing * (Tabs + 1))
    val barWidth = (itemWidth * tabs.size) + (spacing * (tabs.size + 1))

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight),
            shape = CircleShape,
            hazeLayer = HazeLayer.Overlay,
            hazeStyle = GlassTokens.pillStyle(),
            fallbackColor = AppColors.componentOnCard,
            shadowElevation = 8.dp,
            borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.6f)
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
                            shape = CircleShape
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
    onAddClick: (TransactionType?, Long?) -> Unit,
    onAddAccountClick: (() -> Unit)? = null
) {
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val displayTemplates = remember(templates) {
        val starred = templates.filter { it.isStarred }
        if (starred.isNotEmpty()) starred.take(5) else templates.take(5)
    }

    displayTemplates.forEachIndexed { index, template ->
        val icon = getIconByName(categoryMap[template.categoryId]?.iconName)
        FabMenuItem(
            visible = isExpanded,
            label = template.name,
            icon = icon,
            index = index,
            onClick = { onAddClick(template.transactionType, template.id) }
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
        onClick = { onAddClick(TransactionType.TRANSFER, null) }
    )

    FabMenuItem(
        visible = isExpanded,
        label = "New Transaction",
        icon = Icons.Rounded.Add,
        index = if (onAddAccountClick != null) baseIndex + 2 else baseIndex + 1,
        onClick = { onAddClick(TransactionType.EXPENSE, null) }
    )
}

@Composable
fun FabMenuItem(
    visible: Boolean,
    label: String,
    icon: ImageVector,
    index: Int,
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
                scaleIn(initialScale = 0.8f, animationSpec = tween(200, delayMillis = staggerDelay)),
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
            Surface(
                color = AppColors.cardSurfaceProminent,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                interactionSource = interactionSource,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = AppTextStyles.drawerItemLabel,
                    color = AppColors.contentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            SmallFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                modifier = Modifier.size(44.dp),
                interactionSource = interactionSource,
                containerColor = AppColors.secondaryContainer,
                contentColor = AppColors.onSecondaryContainer,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
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
        Box(modifier = Modifier.fillMaxSize().background(AppColors.background)) {
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
