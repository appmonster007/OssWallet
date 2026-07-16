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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
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
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "${label}_translate"
    )
    val shimmerColor = AppColors.insetSurface.copy(alpha = 0.4f)
    val highlightColor = AppColors.insetSurface.copy(alpha = 0.1f)
    
    val colors = listOf(
        shimmerColor,
        highlightColor,
        shimmerColor,
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
            start = Offset(x = translateAnim - 500f, y = translateAnim - 500f),
            end = Offset(x = translateAnim + 500f, y = translateAnim + 500f)
        )
    }
}

@Composable
fun ShimmerCardShell(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = AppColors.cardSurface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, AppColors.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AeonTheme.dimensions.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)
        ) {
            content()
        }
    }
}

@Composable
fun ShimmerHeader(brush: Brush) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)
    ) {
        Box(
            modifier = Modifier
                .size(AeonTheme.dimensions.iconSizeLg)
                .background(brush, CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp, 16.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .size(80.dp, 12.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )
        }
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
    val dims = AeonTheme.dimensions

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = dims.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dims.spacingMd),
        contentPadding = PaddingValues(
            top = dims.spacingMd,
            bottom = dims.spacingXl
        ),
        userScrollEnabled = false
    ) {
        if (showNetWorth) {
            item {
                ShimmerCardShell {
                    ShimmerHeader(brush)
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(dims.spacingSm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp, 36.dp)
                                .background(brush, RoundedCornerShape(8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(140.dp, 14.dp)
                                .background(brush, RoundedCornerShape(4.dp))
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacingLg)
                    ) {
                        repeat(3) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp, 10.dp)
                                        .background(brush, RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(18.dp)
                                        .background(brush, RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCurrency) {
            item {
                ShimmerCardShell {
                    ShimmerHeader(brush)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .background(brush, RoundedCornerShape(dims.componentCornerRadius))
                            )
                        }
                    }
                }
            }
        }

        if (showCashflow) {
            item {
                ShimmerCardShell {
                    ShimmerHeader(brush)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Table Header
                        Row(horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)) {
                            Box(modifier = Modifier.size(50.dp, 12.dp).background(brush, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(70.dp, 12.dp).background(brush, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(70.dp, 12.dp).background(brush, RoundedCornerShape(2.dp)))
                        }
                        // Totals Row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(brush, RoundedCornerShape(6.dp))
                                .graphicsLayer { alpha = 0.3f }
                        )
                        // Per CCY Rows
                        repeat(2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.size(40.dp, 16.dp).background(brush, RoundedCornerShape(4.dp)))
                                Row(horizontalArrangement = Arrangement.spacedBy(dims.spacingLg)) {
                                    Box(modifier = Modifier.size(60.dp, 16.dp).background(brush, RoundedCornerShape(4.dp)))
                                    Box(modifier = Modifier.size(60.dp, 16.dp).background(brush, RoundedCornerShape(4.dp)))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAccounts) {
            item {
                ShimmerCardShell {
                    ShimmerHeader(brush)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.spacingMd)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .background(brush, RoundedCornerShape(dims.componentCornerRadius))
                            )
                        }
                    }
                }
            }
        }

        if (showBudgets) {
            item {
                ShimmerCardShell {
                    ShimmerHeader(brush)
                    repeat(2) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.size(100.dp, 16.dp).background(brush, RoundedCornerShape(4.dp)))
                                Box(modifier = Modifier.size(80.dp, 16.dp).background(brush, RoundedCornerShape(4.dp)))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(brush, RoundedCornerShape(2.dp))
                            )
                        }
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


