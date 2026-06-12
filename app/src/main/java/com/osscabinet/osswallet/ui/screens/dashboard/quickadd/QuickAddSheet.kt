package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.AeonGlassHost
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps.QuickAddAccountStep
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps.QuickAddAmountStep
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps.QuickAddTemplateStep
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
import com.osscabinet.ossWallet.ui.theme.ColorScrimDark
import com.osscabinet.ossWallet.ui.theme.GlassTokens
import com.osscabinet.ossWallet.ui.theme.GlassVariant
import com.osscabinet.ossWallet.ui.theme.HazeLayer
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled
import java.math.BigDecimal

private const val STEP_TEMPLATE = 0
private const val STEP_ACCOUNT = 1
private const val STEP_AMOUNT = 2
private const val TOTAL_STEPS = 3

@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    viewModel: QuickAddViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val glassEnabled = LocalGlassmorphismEnabled.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // State to manage entry/exit transitions
    val visibleState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    // When the exit animation finishes, call finish()
    if (!visibleState.targetState && !visibleState.currentState) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
    }

    // Reset state every time the sheet opens
    LaunchedEffect(Unit) { viewModel.reset() }

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun animateDismiss() {
        hideKeyboard()
        visibleState.targetState = false
    }

    BackHandler {
        when (state.currentStep) {
            STEP_TEMPLATE -> animateDismiss()
            else -> {
                hideKeyboard(); viewModel.goToStep(state.currentStep - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim: Animated alongside the sheet
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorScrimDark)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { animateDismiss() }
                    )
            )
        }

        // Anchor layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                val sheetBody: @Composable () -> Unit = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.divider.copy(alpha = 0.4f))
                            )
                        }

                        QuickAddSheetContent(
                            state = state,
                            templates = templates,
                            categories = categories,
                            viewModel = viewModel,
                            hideKeyboard = ::hideKeyboard,
                            onDismiss = { animateDismiss() }, // Trigger exit animation instead of instant finish
                            onSettled = {}
                        )
                    }
                }

                if (glassEnabled) {
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            ),
                        shape = sheetShape,
                        hazeLayer = HazeLayer.Overlay,
                        hazeStyle = GlassTokens.sheetStyle(),
                        glassVariant = GlassVariant.Sheet,
                        fallbackColor = AppColors.sheetSurface
                    ) {
                        AeonGlassHost(modifier = Modifier.fillMaxWidth()) {
                            sheetBody()
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            ),
                        shape = sheetShape,
                        color = AppColors.cardSurface,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        sheetBody()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddSheetContent(
    state: QuickAddState,
    templates: List<Template>,
    categories: List<Category>,
    viewModel: QuickAddViewModel,
    hideKeyboard: () -> Unit,
    onDismiss: () -> Unit,
    onSettled: () -> Unit
) {
    var isSheetSettled by remember { mutableStateOf(false) }
    val windowInfo = LocalWindowInfo.current

    val navBtnSize  = 28.dp
    val navIconSize = 14.dp
    val navVPad     = 3.dp
    val dotActive   = 6.dp
    val dotInactive = 5.dp

    fun goBack() {
        when (state.currentStep) {
            STEP_TEMPLATE -> {
                hideKeyboard(); onDismiss()
            }

            else -> {
                hideKeyboard(); viewModel.goToStep(state.currentStep - 1)
            }
        }
    }

    fun goNext() {
        hideKeyboard()
        viewModel.goToStep(state.currentStep + 1)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (!isSheetSettled) {
                    val windowHeight = windowInfo.containerSize.height
                    val selfHeight = coords.size.height
                    val yInWindow = coords.positionInWindow().y
                    // When the sheet reaches the bottom of the screen (or near it)
                    if (yInWindow >= (windowHeight - selfHeight).toFloat() - 5f) {
                        isSheetSettled = true
                        onSettled()
                    }
                }
            }
    ) {
        // Back / step dots / next row
        val canGoNext = when (state.currentStep) {
            STEP_TEMPLATE -> state.selectedTemplate != null || state.selectedCategoryId != null
            STEP_ACCOUNT -> state.selectedAccountId != null
            STEP_AMOUNT -> state.amount > BigDecimal.ZERO
            else -> false
        }
        val isLastStep = state.currentStep == STEP_AMOUNT

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = navVPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Surface(
                onClick = ::goBack,
                shape = CircleShape,
                color = AppColors.recessedSurface,
                modifier = Modifier.size(navBtnSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.contentSecondary,
                        modifier = Modifier.size(navIconSize)
                    )
                }
            }

            // Step dots centred
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(TOTAL_STEPS) { index ->
                    val isActive = state.currentStep == index
                    Box(
                        modifier = Modifier
                            .size(if (isActive) dotActive else dotInactive)
                            .clip(CircleShape)
                            .background(
                                if (isActive) AppColors.primary
                                else AppColors.divider.copy(alpha = 0.4f)
                            )
                    )
                    if (index < TOTAL_STEPS - 1) Spacer(modifier = Modifier.width(6.dp))
                }
            }

            // Next / Save button
            if (isLastStep) {
                Surface(
                    onClick = {
                        if (!state.isSaving) {
                            viewModel.saveTransaction(onSuccess = onDismiss)
                        }
                    },
                    shape = CircleShape,
                    color = if (state.selectedAccountId != null) AppColors.primary
                    else AppColors.divider.copy(alpha = 0.5f),
                    modifier = Modifier.size(navBtnSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(navIconSize),
                                color = AppColors.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Done,
                                contentDescription = "Save",
                                tint = AppColors.onPrimary,
                                modifier = Modifier.size(navIconSize)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    onClick = { if (canGoNext) goNext() },
                    shape = CircleShape,
                    color = if (canGoNext) AppColors.primary
                    else AppColors.divider.copy(alpha = 0.5f),
                    modifier = Modifier.size(navBtnSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next",
                            tint = AppColors.onPrimary,
                            modifier = Modifier.size(navIconSize)
                        )
                    }
                }
            }
        }

        AeonGlassHost {
            AnimatedContent(
                targetState = state.currentStep,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(280)) { it } + fadeIn(tween(200))).togetherWith(
                            slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(200))
                        )
                    } else {
                        (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(200))).togetherWith(
                            slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200))
                        )
                    }
                },
                label = "QuickAddStep"
            ) { step ->
                when (step) {
                    STEP_TEMPLATE -> QuickAddTemplateStep(
                        templates = templates,
                        categories = categories,
                        selectedTemplateId = state.selectedTemplate?.id,
                        selectedCategoryId = state.selectedCategoryId,
                        isSheetSettled = isSheetSettled,
                        onTemplateSelected = { template ->
                            viewModel.onTemplateSelected(template)
                        },
                        onCategorySelected = { category ->
                            viewModel.onCategorySelected(category)
                        }
                    )

                    STEP_ACCOUNT -> QuickAddAccountStep(
                        accounts = state.accounts,
                        selectedAccountId = state.selectedAccountId,
                        transactionType = state.transactionType,
                        isStepActive = state.currentStep == STEP_ACCOUNT,
                        onAccountTapped = {
                            hideKeyboard()
                            viewModel.onAccountTapped(it)
                        },
                        onToggleType = { viewModel.toggleTransactionType() }
                    )

                    STEP_AMOUNT -> {
                        val selectedAccount = state.accounts.find { it.id == state.selectedAccountId }
                        val selectedCategory = categories.find { it.id == state.selectedCategoryId }
                        QuickAddAmountStep(
                            amount = state.amount,
                            template = state.selectedTemplate,
                            category = selectedCategory,
                            account = selectedAccount,
                            selectedCurrency = state.selectedCurrency,
                            onCurrencySelected = { viewModel.onCurrencySelected(it) },
                            onAmountChange = viewModel::onAmountChanged,
                            onDone = {
                                if (!state.isSaving) {
                                    viewModel.saveTransaction(onSuccess = onDismiss)
                                }
                            }
                        )
                    }

                    else -> {}
                }
            }
        }

        // Step label hint
        val stepLabel = when (state.currentStep) {
            STEP_TEMPLATE -> "Pick a template or category"
            STEP_ACCOUNT -> "Choose account & transaction type"
            STEP_AMOUNT -> "Enter amount"
            else -> ""
        }
        Text(
            text = stepLabel,
            style = AppTextStyles.chartAxisLabel,
            color = AppColors.contentSecondary.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}
