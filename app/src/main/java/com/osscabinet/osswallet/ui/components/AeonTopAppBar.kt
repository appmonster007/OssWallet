package com.osscabinet.ossWallet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AeonSize
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null,
    isTopLevel: Boolean = onMenuClick != null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    titleContent: @Composable (() -> Unit)? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    val topBarHeight = AeonSize.D64.value
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val totalHeaderHeight = topBarHeight + statusBarHeight

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Header Background Glass (Addressing Visual Issues 1, 2, 3, 4)
        // Provides a subtle blur over the status bar and top bar area to prevent contamination
        // and sharp cuts in the list content scrolling under.
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeaderHeight),
            shape = RoundedCornerShape(0.dp), // Full bleed for the header strip
            hazeLayer = HazeLayer.Surface,
            hazeStyle = GlassTokens.style(),
            fallbackColor = Color.Transparent,
            shadowElevation = 0.dp,
            borderAlpha = 0f // No rim for the base background strip
        ) {
            // Spacer ensures the GlassSurface occupies the full measured header area
            Spacer(modifier = Modifier.fillMaxSize())
        }

        // 2. The Content Row (The "Floating Pills" Aesthetic)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .height(topBarHeight)
                .padding(horizontal = AeonSize.D12.value),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Pill: [Icon + Title/Subtitle]
            val hasNavigation = (isTopLevel && onMenuClick != null) || onBackClick != null
            val hasTitle = title.isNotEmpty() || titleContent != null

            if (hasNavigation || hasTitle) {
                GlassSurface(
                    shape = CircleShape,
                    hazeLayer = HazeLayer.Overlay,
                    hazeStyle = GlassTokens.pillStyle(),
                    fallbackColor = AppColors.componentOnCard,
                    shadowElevation = AeonSize.D8.value
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            start = if (hasNavigation) AeonSize.D4.value else AeonSize.D16.value,
                            end = AeonSize.D16.value,
                            top = AeonSize.D4.value,
                            bottom = AeonSize.D4.value
                        )
                    ) {
                        if (isTopLevel && onMenuClick != null) {
                            AppBarIconButton(icon = Icons.Rounded.Menu, onClick = onMenuClick)
                            Spacer(modifier = Modifier.width(AeonSize.D4.value))
                        } else if (onBackClick != null) {
                            AppBarIconButton(
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                onClick = onBackClick
                            )
                            Spacer(modifier = Modifier.width(AeonSize.D4.value))
                        }

                        Column(
                            modifier = Modifier.padding(vertical = AeonSize.D4.value),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (titleContent != null) {
                                titleContent()
                            } else if (title.isNotEmpty()) {
                                Text(
                                    title,
                                    style = AppTextStyles.screenTitle.copy(
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        letterSpacing = 0.sp
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.onScreenBackground
                                )
                            }
                            if (subtitleContent != null) {
                                subtitleContent()
                            } else if (subtitle != null) {
                                Text(
                                    subtitle,
                                    style = AppTextStyles.caption.copy(
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = AppColors.contentSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            // Right Pill: [Actions]
            if (actions != null) {
                GlassSurface(
                    shape = CircleShape,
                    hazeLayer = HazeLayer.Overlay,
                    hazeStyle = GlassTokens.pillStyle(),
                    fallbackColor = AppColors.componentOnCard,
                    shadowElevation = AeonSize.D8.value
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(
                            horizontal = AeonSize.D4.value,
                            vertical = AeonSize.D4.value
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AeonSize.D2.value)
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides AppColors.onScreenBackground
                        ) {
                            actions()
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }
    }
}

@Composable
fun AppBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(AeonSize.D40.value)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(AeonSize.D22.value),
            tint = AppColors.onScreenBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonTopAppBarPreview() {
     OssWalletTheme {
        AeonHazeProvider(backgroundColor = Color(0xFF1A1C1E)) { hazeLayers ->
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background elements to show off glass blur
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AeonSize.D40.value)
                        .hazeSource(hazeLayers.states[HazeLayer.Overlay]),
                    verticalArrangement = Arrangement.spacedBy(AeonSize.D12.value)
                ) {
                    repeat(10) { i ->
                        Text(
                            "AEON GLASS ARCHITECTURE ".repeat(2),
                            style = AppTextStyles.heroAmount.copy(
                                fontSize = 24.sp,
                                color = if (i % 2 == 0) AppColors.primary.copy(alpha = 0.15f) else AppColors.secondary.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.padding(horizontal = AeonSize.D16.value)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(AeonSize.D8.value)) {
                    // Full AppBar
                    AeonTopAppBar(
                        title = "Dashboard",
                        onMenuClick = {},
                        isTopLevel = true,
                        actions = {
                            AppBarIconButton(icon = Icons.Rounded.Search, onClick = {})
                        }
                    )
                    // No Actions
                    AeonTopAppBar(
                        title = "Transactions",
                        subtitle = "Showing all results",
                        onBackClick = {},
                        isTopLevel = false
                    )
                    // No Nav, with Actions
                    AeonTopAppBar(
                        title = "Quick Settings",
                        actions = {
                            AppBarIconButton(icon = Icons.Rounded.Search, onClick = {})
                        }
                    )
                }
            }
        }
    }
}
