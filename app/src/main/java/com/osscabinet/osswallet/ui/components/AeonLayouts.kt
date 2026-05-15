package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osscabinet.ossWallet.ui.theme.AeonTheme
import androidx.compose.ui.input.pointer.pointerInput

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
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
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
                shape = RoundedCornerShape(topStart = AeonTheme.dimensions.cardCornerRadius, bottomStart = AeonTheme.dimensions.cardCornerRadius),
                color = Color(0xFFC2D2FF),
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
                color = MaterialTheme.colorScheme.surface,
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
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Back")
                        }
                        Text(
                            "Additional Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        content()
                    }
                }
            }
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
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = AeonTheme.dimensions.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingXl),
        contentPadding = PaddingValues(top = AeonTheme.dimensions.spacingLg, bottom = AeonTheme.dimensions.spacingXl)
    ) {
        if (showNetWorth) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingSm)) {
                    Box(modifier = Modifier.size(150.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                    Box(modifier = Modifier.size(250.dp, 48.dp).background(brush, RoundedCornerShape(8.dp)))
                    Box(modifier = Modifier.size(100.dp, 24.dp).background(brush, RoundedCornerShape(16.dp)))
                }
            }
        }
        if (showCurrency) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                    repeat(3) {
                        Box(modifier = Modifier.size(120.dp, 60.dp).background(brush, RoundedCornerShape(AeonTheme.dimensions.containerCornerRadius)))
                    }
                }
            }
        }
        if (showCashflow) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(brush, RoundedCornerShape(AeonTheme.dimensions.dialogCornerRadius)))
            }
        }
        if (showAccounts) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.size(100.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                        Box(modifier = Modifier.size(60.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingMd)) {
                        Box(modifier = Modifier.weight(1f).height(120.dp).background(brush, RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)))
                        Box(modifier = Modifier.weight(1f).height(120.dp).background(brush, RoundedCornerShape(AeonTheme.dimensions.cardCornerRadius)))
                    }
                }
            }
        }
        if (showBudgets) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AeonTheme.dimensions.spacingLg)) {
                    Box(modifier = Modifier.size(120.dp, 20.dp).background(brush, RoundedCornerShape(4.dp)))
                    repeat(2) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(brush, RoundedCornerShape(16.dp)))
                    }
                }
            }
        }
    }
}
