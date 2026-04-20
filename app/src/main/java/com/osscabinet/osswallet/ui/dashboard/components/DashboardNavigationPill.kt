package com.osscabinet.ossWallet.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DashboardNavigationPill(
    pagerState: PagerState,
    tabs: List<Pair<ImageVector, String>>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val tabCount = tabs.size

    Surface(
        modifier = modifier
            .padding(bottom = 24.dp)
            .height(56.dp)
            .width(160.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxSize()
        ) {
            val tabWidth = maxWidth / tabCount
            
            // Sliding indicator
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * pagerState.currentPage,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicatorOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, (icon, contentDescription) ->
                    val selected = pagerState.currentPage == index
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary 
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "contentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = contentDescription,
                            tint = contentColor
                        )
                    }
                }
            }
        }
    }
}
