package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.rounded.CoffeeMaker
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.osscabinet.ossWallet.data.local.entity.TemplateEntity

@Composable
fun AeonExpandableFab(
    onAddClick: (Int?, String?) -> Unit,
    templates: List<TemplateEntity>,
    modifier: Modifier = Modifier,
    fabPaddingBottom: androidx.compose.ui.unit.Dp = 16.dp
) {
    var isFabExpanded by remember { mutableStateOf(false) }

    val displayTemplates = remember(templates) {
        val starred = templates.filter { it.isStarred }
        val finalTemplates = if (starred.isNotEmpty()) {
            starred
        } else if (templates.isNotEmpty()) {
            templates.take(5)
        } else {
            emptyList()
        }

        finalTemplates.map {
            Triple(it.name, when(it.transactionType) {
                1 -> Icons.Rounded.Payments
                2 -> Icons.Rounded.SwapHoriz
                else -> Icons.Rounded.ShoppingCart
            }, it.transactionType)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background dimming
        AnimatedVisibility(
            visible = isFabExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFabExpanded = false }
            )
        }

        // Expandable FAB Menu
        Box(
            modifier = modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = fabPaddingBottom)
                .zIndex(2f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 72.dp, end = 4.dp)
            ) {
                displayTemplates.forEach { (name, icon, type) ->
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 4.dp,
                                onClick = { 
                                    isFabExpanded = false
                                    onAddClick(type, name)
                                }
                            ) {
                                Text(
                                    "Template: $name",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            AeonFloatingActionButton(
                                onClick = { 
                                    isFabExpanded = false
                                    onAddClick(type, name)
                                },
                                icon = icon,
                                modifier = Modifier.size(40.dp),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Transfer
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 4.dp,
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(2, null)
                            }
                        ) {
                            Text(
                                "Transfer",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        AeonFloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(2, null)
                            },
                            modifier = Modifier.size(40.dp),
                            icon = Icons.Default.SyncAlt,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // New Transaction
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 4.dp,
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(0, null)
                            }
                        ) {
                            Text(
                                "New Transaction",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        AeonFloatingActionButton(
                            onClick = { 
                                isFabExpanded = false
                                onAddClick(0, null)
                            },
                            modifier = Modifier.size(40.dp),
                            icon = Icons.Default.Add,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            val rotation by animateFloatAsState(
                targetValue = if (isFabExpanded) 45f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "FabRotation"
            )

            AeonFloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                modifier = Modifier
                    .zIndex(3f)
                    .graphicsLayer { rotationZ = rotation },
                icon = Icons.Default.Add,
                contentDescription = if (isFabExpanded) "Close" else "Add"
            )
        }
    }
}
