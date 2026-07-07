package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

@Composable
fun scrollBottomPadding(): Dp {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    return (screenHeightDp / 2).dp
}

@Composable
fun AeonSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = AppTextStyles.managementItemTitle.copy(
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.contentPrimary
            )
        )
        trailing?.invoke()
    }
}

@Composable
fun DetailDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onSwipeOpen: () -> Unit,
    tabAlignment: Alignment = Alignment.CenterEnd,
    tabOffsetY: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val offset by animateDpAsState(if (isOpen) 0.dp else screenWidth, label = "drawerOffset")

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab indicator on the right edge when closed
        if (!isOpen) {
            Surface(
                modifier = Modifier
                    .align(tabAlignment)
                    .offset(x = 8.dp, y = tabOffsetY) // Hide slightly
                    .width(48.dp)
                    .height(80.dp)
                    .clickable { onSwipeOpen() }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount < -20) {
                                change.consume()
                                onSwipeOpen()
                            }
                        }
                    },
                shape = RoundedCornerShape(
                    topStart = AeonTheme.dimensions.cardCornerRadius,
                    bottomStart = AeonTheme.dimensions.cardCornerRadius
                ),
                color = AppColors.accentContainer,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Open Details",
                        modifier = Modifier.padding(end = AeonTheme.dimensions.listPadding)
                    )
                }
            }
        }

        // Drawer content overlay
        if (isOpen || offset < screenWidth) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(screenWidth)
                    .offset(x = offset)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 20) {
                                change.consume()
                                onClose()
                            }
                        }
                    },
                color = AppColors.panelSurface,
                shadowElevation = 24.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AeonTheme.dimensions.screenPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Back"
                            )
                        }
                        Text(
                            "Additional Details",
                            style = AppTextStyles.screenSubTitle,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun rememberShimmerBrush(horizontal: Boolean = false, label: String = "shimmer"): Brush {
    val transition = rememberInfiniteTransition(label = label)
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "${label}_translate"
    )
    val colors = listOf(
        AppColors.insetSurface.copy(alpha = 0.6f),
        AppColors.insetSurface.copy(alpha = 0.2f),
        AppColors.insetSurface.copy(alpha = 0.6f),
    )
    return if (horizontal) {
        Brush.linearGradient(
            colors = colors,
            start = Offset(x = translateAnim - 300f, y = 0f),
            end = Offset(x = translateAnim + 300f, y = 0f)
        )
    } else {
        Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset(x = translateAnim, y = translateAnim)
        )
    }
}

@Composable
fun AeonShimmer(
    innerPadding: PaddingValues,
    showNetWorth: Boolean = true,
    showCurrency: Boolean = true,
    showCashflow: Boolean = true,
    showAccounts: Boolean = true,
    showBudgets: Boolean = false
) {
    val brush = rememberShimmerBrush(label = "aeon_shimmer")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = AeonTheme.dimensions.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl),
        contentPadding = PaddingValues(
            top = AeonTheme.dimensions.spacingLg,
            bottom = AeonTheme.dimensions.spacingXl
        )
    ) {
        if (showNetWorth) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                    Box(
                        modifier = Modifier
                            .size(150.dp, 20.dp)
                            .background(brush, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(250.dp, 48.dp)
                            .background(brush, RoundedCornerShape(8.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp, 24.dp)
                            .background(brush, RoundedCornerShape(16.dp))
                    )
                }
            }
        }
        if (showCurrency) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(120.dp, 60.dp)
                                .background(
                                    brush,
                                    RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)
                                )
                        )
                    }
                }
            }
        }
        if (showCashflow) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            brush,
                            RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius)
                        )
                )
            }
        }
        if (showAccounts) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp, 20.dp)
                                .background(brush, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(60.dp, 20.dp)
                                .background(brush, RoundedCornerShape(4.dp))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .background(
                                    brush,
                                    RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .background(
                                    brush,
                                    RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
                                )
                        )
                    }
                }
            }
        }
        if (showBudgets) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                    Box(
                        modifier = Modifier
                            .size(120.dp, 20.dp)
                            .background(brush, RoundedCornerShape(4.dp))
                    )
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(brush, RoundedCornerShape(16.dp))
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AeonSectionHeaderPreview() {
    OssWalletTheme {
        AeonSectionHeader(
            title = "Section Title",
            modifier = Modifier.padding(16.dp),
            trailing = {
                Text("See All", style = AppTextStyles.metaLabel, color = AppColors.primary)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AeonShimmerPreview() {
    OssWalletTheme {
        AeonShimmer(innerPadding = PaddingValues(0.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DetailDrawerPreview() {
    OssWalletTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            DetailDrawer(
                isOpen = true,
                onClose = { },
                onSwipeOpen = { }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Content in drawer")
                }
            }
        }
    }
}


