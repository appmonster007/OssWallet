package com.osscabinet.ossWallet.ui.components.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.osscabinet.ossWallet.R
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Standardized Scaffold for all Entry/Creation screens in OssWallet.
 * Following the "Architect Slate" design specification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AeonEntryScaffold(
    title: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    canSave: Boolean,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    onResetClick: (() -> Unit)? = null,
    containerColor: Color = AppColors.screenBackground,
    content: @Composable (PaddingValues) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = AppTextStyles.screenTitle.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (onResetClick != null) {
                        IconButton(onClick = onResetClick) {
                            Icon(
                                Icons.Rounded.RestartAlt,
                                contentDescription = stringResource(R.string.action_reset)
                            )
                        }
                    }
                    if (isEditMode && onDeleteClick != null) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteClick()
                        }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = AppColors.error
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveClick()
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            Icons.Rounded.Done,
                            contentDescription = stringResource(R.string.action_save),
                            tint = if (canSave) {
                                AppColors.primary
                            } else {
                                AppColors.contentSecondary.copy(alpha = 0.38f)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = containerColor
                )
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Preview(showBackground = true)
@Composable
fun AeonEntryScaffoldPreview() {
    OssWalletTheme {
        AeonEntryScaffold(
            title = "New Transaction",
            onBackClick = {},
            onSaveClick = {},
            canSave = true
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Text("Content goes here")
            }
        }
    }
}
