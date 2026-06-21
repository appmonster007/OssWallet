package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.ui.components.AeonGlassHost
import com.osscabinet.ossWallet.ui.components.GlassSurface
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps.QuickAddAccountStep
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps.QuickAddAmountStep
import com.osscabinet.ossWallet.ui.screens.dashboard.quickadd.steps.QuickAddTemplateStep
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.AppTextStyles
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

    // Predictive Back Handling (Modern SDK) — system/gesture back always dismisses
    // the whole sheet, regardless of step. Stepping back is done via the in-sheet
    // back chevron in the footer.
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = visibleState.targetState) { progress ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            animateDismiss()
        } catch (_: Exception) {
            backProgress = 0f
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
//                    .background(ColorScrimDark)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { animateDismiss() }
                    )
            )
        }

        // Anchor layer — capped below the status bar so the sheet can grow with its
        // grid content but never overlaps the system status bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.ime)
                        .union(WindowInsets.navigationBars)
                )
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val scale = 1f - (backProgress * 0.05f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (backProgress * 0.2f)
                    }
            ) {
                val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                val sheetBody: @Composable () -> Unit = {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val maxSheetHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxSheetHeight)
                        ) {
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
                                onSettled = {},
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
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
    onSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSheetSettled by remember { mutableStateOf(false) }
    val windowInfo = LocalWindowInfo.current

    fun goBackStep() {
        if (state.currentStep > STEP_TEMPLATE) {
            hideKeyboard(); viewModel.goToStep(state.currentStep - 1)
        }
    }

    fun goNext() {
        hideKeyboard()
        viewModel.goToStep(state.currentStep + 1)
    }

    val canGoNext = when (state.currentStep) {
        STEP_TEMPLATE -> state.selectedTemplate != null || state.selectedCategoryId != null
        STEP_ACCOUNT -> state.selectedAccountId != null
        STEP_AMOUNT -> state.amount > BigDecimal.ZERO
        else -> false
    }
    val isLastStep = state.currentStep == STEP_AMOUNT
    val title = when (state.currentStep) {
        STEP_TEMPLATE -> "What did you spend on?"
        STEP_ACCOUNT -> "Which account?"
        STEP_AMOUNT -> "How much?"
        else -> ""
    }
    val ctaLabel = when (state.currentStep) {
        STEP_TEMPLATE -> "ACCOUNT"
        STEP_ACCOUNT -> "AMOUNT"
        STEP_AMOUNT -> "SAVE"
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (!isSheetSettled) {
                    val windowHeight = windowInfo.containerSize.height
                    val selfHeight = coords.size.height
                    val yInWindow = coords.positionInWindow().y
                    if (yInWindow >= (windowHeight - selfHeight).toFloat() - 5f) {
                        isSheetSettled = true
                        onSettled()
                    }
                }
            }
    ) {
        // ── Editorial header ───────────────────────────────────────────────────
        EditorialHeader(title = title)

        // ── Step body (the flexible region) ────────────────────────────────────
        AeonGlassHost(modifier = Modifier.weight(1f, fill = false)) {
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
                        structureTypes = state.structureTypes,
                        selectedAccountId = state.selectedAccountId,
                        entryMode = state.entryMode,
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
                            accountBalance = state.accountBalances[state.selectedAccountId] ?: BigDecimal.ZERO,
                            entryMode = state.entryMode,
                            exchangeRates = state.exchangeRates,
                            baseCurrency = state.baseCurrency,
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

        // ── Footer: back chevron · step dots · champagne CTA ───────────────────
        QuickAddFooter(
            currentStep = state.currentStep,
            showBack = state.currentStep > STEP_TEMPLATE,
            ctaLabel = ctaLabel,
            ctaEnabled = canGoNext,
            isLastStep = isLastStep,
            isSaving = state.isSaving,
            onBack = ::goBackStep,
            onCta = {
                if (isLastStep) {
                    if (!state.isSaving) viewModel.saveTransaction(onSuccess = onDismiss)
                } else if (canGoNext) {
                    goNext()
                }
            }
        )
    }
}

@Composable
private fun EditorialHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(1.dp)
                    .background(AppColors.accent.copy(alpha = 0.7f))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "NEW ENTRY",
                style = AppTextStyles.sectionOverline,
                color = AppColors.contentSecondary.copy(alpha = 0.85f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = AppTextStyles.screenTitle.copy(
                fontWeight = FontWeight.Light,
                fontSize = 18.sp
            ),
            color = AppColors.contentPrimary
        )
    }
}

@Composable
private fun QuickAddFooter(
    currentStep: Int,
    showBack: Boolean,
    ctaLabel: String,
    ctaEnabled: Boolean,
    isLastStep: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onCta: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = AppColors.recessedSurface,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Previous step",
                        tint = AppColors.contentSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }

        // Step dots
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(TOTAL_STEPS) { index ->
                val isActive = currentStep == index
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (isActive) 20.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) AppColors.accent
                            else AppColors.divider.copy(alpha = 0.5f)
                        )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Champagne CTA
        Surface(
            onClick = onCta,
            shape = RoundedCornerShape(20.dp),
            color = if (ctaEnabled) AppColors.accent else AppColors.divider.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = ctaLabel,
                    style = AppTextStyles.sectionOverline,
                    color = if (ctaEnabled) AppColors.onAccent else AppColors.contentSecondary
                )
                if (isLastStep && isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        color = AppColors.onAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isLastStep) Icons.Rounded.Done
                        else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = if (ctaEnabled) AppColors.onAccent else AppColors.contentSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
