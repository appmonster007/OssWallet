package com.osscabinet.ossWallet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles

data class AeonTimelineOption(val label: String, val icon: ImageVector)

@Composable
fun AeonSegmentedControl(
    options: List<AeonTimelineOption>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppColors.cardSurface,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth().height(60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedOption == index
                val backgroundColor by animateColorAsState(
                    if (isSelected) AppColors.primary else AppColors.transparent,
                    label = "bgColor"
                )
                val contentColor by animateColorAsState(
                    if (isSelected) AppColors.onPrimary else AppColors.contentSecondary,
                    label = "contentColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = contentColor
                        )
                        Text(
                            text = option.label,
                            style = AppTextStyles.transactionSwipeAction.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonSegmentedControlPreview() {
    val options = listOf(
        AeonTimelineOption("Daily", Icons.Default.ViewDay),
        AeonTimelineOption("Weekly", Icons.Default.ViewWeek),
        AeonTimelineOption("Monthly", Icons.Default.CalendarViewMonth)
    )
    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AeonSegmentedControl(
                options = options,
                selectedOption = 1,
                onOptionSelected = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AeonDropdown(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = labelProvider(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { 
                Text(
                    label.uppercase(),
                    style = AppTextStyles.transactionSwipeAction.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                ) 
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primary,
                unfocusedBorderColor = AppColors.divider,
                focusedLabelColor = AppColors.primary,
                unfocusedContainerColor = AppColors.insetSurface.copy(alpha = 0.2f),
                focusedContainerColor = AppColors.insetSurface.copy(alpha = 0.1f)
            ),
            textStyle = AppTextStyles.listItemTitle
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(AppColors.panelSurface)
                .clip(RoundedCornerShape(16.dp))
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                DropdownMenuItem(
                    text = { 
                        Text(
                            labelProvider(option),
                            style = AppTextStyles.listItemTitle.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            ),
                            color = if (isSelected) AppColors.primary else AppColors.contentPrimary
                        ) 
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AeonDropdownPreview() {
    val options = listOf("Monthly", "Weekly", "Daily", "Yearly")
    var selectedOption by remember { mutableStateOf(options[0]) }

    com.osscabinet.ossWallet.ui.theme.OssWalletTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AeonDropdown(
                label = "Period",
                options = options,
                selectedOption = selectedOption,
                onOptionSelected = { selectedOption = it },
                labelProvider = { it }
            )
        }
    }
}
