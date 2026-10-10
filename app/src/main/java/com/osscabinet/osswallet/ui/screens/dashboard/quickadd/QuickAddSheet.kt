package com.osscabinet.ossWallet.ui.screens.dashboard.quickadd

import com.osscabinet.ossWallet.ui.theme.GlassElement
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.osscabinet.ossWallet.domain.model.Account
import com.osscabinet.ossWallet.domain.model.AccountRef
import com.osscabinet.ossWallet.domain.model.AccountStructureType
import com.osscabinet.ossWallet.domain.model.AmountEditingSide
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.EntryMode
import com.osscabinet.ossWallet.domain.model.Template
import com.osscabinet.ossWallet.domain.service.management.TransactionEntryService
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
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import java.math.BigDecimal

private const val STEP_TEMPLATE = 0
private const val STEP_ACCOUNT = 1
private const val STEP_AMOUNT = 2
private const val TOTAL_STEPS = 3

@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    initialTemplate: Template? = null,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var saveSucceeded by remember { mutableStateOf(false) }

    // When launched with a pre-selected template, suppress step content until
    // openWithTemplate has propagated to `state`. Step -1 is a sentinel:
    // the when(step) else->{} branch renders nothing so QuickAddTemplateStep
    // never enters composition and its focus requester never fires.
    //
    // Key insight: `state.selectedTemplate` and `state.currentStep` are fields of
    // the same QuickAddState object emitted by the same StateFlow. They update
    // atomically in the same recomposition, eliminating the one-frame gap that a
    // separate `templateApplied` boolean would have (it could flip true one frame
    // before `state.currentStep` updates, briefly exposing step 0).
    val effectiveState = if (initialTemplate != null && state.selectedTemplate == null) {
        state.copy(currentStep = -1)
    } else {
        state
    }

    QuickAddSheetInternal(
        state = effectiveState,
        sections = sections,
        categories = categories,
        onDismiss = onDismiss,
        onReset = if (initialTemplate != null) {
            { viewModel.openWithTemplate(initialTemplate) }
        } else {
            viewModel::reset
        },
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onTemplateSelected = viewModel::onTemplateSelected,
        onSuggestionSelected = viewModel::onSuggestionSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onAccountTapped = viewModel::onAccountTapped,
        onUpdateAccount = viewModel::updateAccount,
        onUpdateCategory = viewModel::updateCategory,
        onToggleType = viewModel::toggleTransactionType,
        onToggleTransfer = viewModel::toggleTransferMode,
        onSwapAccounts = viewModel::onSwapAccounts,
        onAmountChange = viewModel::onAmountChanged,
        onCurrencySelected = viewModel::onCurrencySelected,
        onSetAmountFocus = viewModel::setAmountFocus,
        onSetAccountFocus = viewModel::setAccountFocus,
        onToggleAmountOverride = viewModel::toggleAmountOverride,
        onSaveTransaction = { viewModel.saveTransaction(onSuccess = { saveSucceeded = true }) },
        goToStep = viewModel::goToStep,
        isSaveSuccess = saveSucceeded
    )
}

@Composable
internal fun QuickAddSheetInternal(
    state: QuickAddState,
    sections: List<QuickAddSection>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTemplateSelected: (Template) -> Unit,
    onSuggestionSelected: (QuickAddResult.SuggestionItem) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onAccountTapped: (String, Boolean) -> Unit,
    onUpdateAccount: (String, Boolean) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onToggleType: () -> Unit,
    onToggleTransfer: () -> Unit,
    onSwapAccounts: () -> Unit,
    onAmountChange: (BigDecimal) -> Unit,
    onCurrencySelected: (String, Boolean) -> Unit,
    onSetAmountFocus: (AmountEditingSide) -> Unit,
    onSetAccountFocus: (AmountEditingSide) -> Unit,
    onToggleAmountOverride: () -> Unit,
    onSaveTransaction: () -> Unit,
    goToStep: (Int) -> Unit,
    isSaveSuccess: Boolean = false
) {
    val glassEnabled = LocalGlassmorphismEnabled.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isPreview = LocalInspectionMode.current

    // State to manage entry/exit transitions. Start "true" in previews so the sheet is visible.
    val visibleState = remember {
        MutableTransitionState(initialState = isPreview).apply {
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
    LaunchedEffect(Unit) {
        onReset()
    }

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun animateDismiss() {
        hideKeyboard()
        visibleState.targetState = false
    }

    LaunchedEffect(isSaveSuccess) {
        if (isSaveSuccess) {
            kotlinx.coroutines.delay(650)
            animateDismiss()
        }
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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { animateDismiss() }
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
                                sections = sections,
                                categories = categories,
                                onSearchQueryChanged = onSearchQueryChanged,
                                onTemplateSelected = onTemplateSelected,
                                onSuggestionSelected = onSuggestionSelected,
                                onCategorySelected = onCategorySelected,
                                onAccountTapped = onAccountTapped,
                                onUpdateAccount = onUpdateAccount,
                                onUpdateCategory = onUpdateCategory,
                                onToggleType = onToggleType,
                                onToggleTransfer = onToggleTransfer,
                                onSwapAccounts = onSwapAccounts,
                                onAmountChange = onAmountChange,
                                onCurrencySelected = onCurrencySelected,
                                onSetAmountFocus = onSetAmountFocus,
                                onSetAccountFocus = onSetAccountFocus,
                                onToggleAmountOverride = onToggleAmountOverride,
                                onSaveTransaction = onSaveTransaction,
                                goToStep = goToStep,
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
                        element = GlassElement.SHEET,
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

        // Success overlay — shown after save, auto-dismissed after delay
        AnimatedVisibility(
            visible = isSaveSuccess,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.surface.copy(alpha = 0.85f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = null,
                    tint = AppColors.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickAddSheetContent(
    state: QuickAddState,
    sections: List<QuickAddSection>,
    categories: List<Category>,
    onSearchQueryChanged: (String) -> Unit,
    onTemplateSelected: (Template) -> Unit,
    onSuggestionSelected: (QuickAddResult.SuggestionItem) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onAccountTapped: (String, Boolean) -> Unit,
    onUpdateAccount: (String, Boolean) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onToggleType: () -> Unit,
    onToggleTransfer: () -> Unit,
    onSwapAccounts: () -> Unit,
    onAmountChange: (BigDecimal) -> Unit,
    onCurrencySelected: (String, Boolean) -> Unit,
    onSetAmountFocus: (AmountEditingSide) -> Unit,
    onSetAccountFocus: (AmountEditingSide) -> Unit,
    onToggleAmountOverride: () -> Unit,
    onSaveTransaction: () -> Unit,
    goToStep: (Int) -> Unit,
    hideKeyboard: () -> Unit,
    onDismiss: () -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSheetSettled by remember { mutableStateOf(false) }
    val windowInfo = LocalWindowInfo.current

    fun goBackStep() {
        if (state.currentStep > STEP_TEMPLATE) {
            goToStep(state.currentStep - 1)
        }
    }

    fun goNext() {
        if (state.currentStep == STEP_ACCOUNT) {
            hideKeyboard()
        }
        goToStep(state.currentStep + 1)
    }

    val canGoNext = when (state.currentStep) {
        STEP_TEMPLATE -> (state.selectedTemplate != null) || (state.selectedCategoryId != null)
        STEP_ACCOUNT -> {
            TransactionEntryService.validateAccountSelection(
                entryMode = state.entryMode,
                source = state.selectedAccount,
                target = state.selectedToAccount
            ).isValid
        }

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
    val subtext = when (state.currentStep) {
        STEP_ACCOUNT -> {
            if (state.entryMode == EntryMode.TRANSFER) {
                val from = state.accounts.find { it.id == state.selectedAccountId }?.name ?: "?"
                val to = state.accounts.find { it.id == state.selectedToAccountId }?.name ?: "?"
                "$from → $to"
            } else {
                state.accounts.find { it.id == state.selectedAccountId }?.name
            }
        }

        else -> null
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
            },
    ) {
        // ── Editorial header ───────────────────────────────────────────────────
        EditorialHeader(title = title, subtext = subtext)

        // ── Step body (the flexible region) ────────────────────────────────────
        AeonGlassHost(modifier = Modifier.weight(1f, fill = false)) {
            AnimatedContent(
                targetState = state.currentStep,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    if (initialState == -1) {
                        // Sentinel → real step: sheet is opening directly at this step, no slide
                        EnterTransition.None togetherWith ExitTransition.None
                    } else if (targetState > initialState) {
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
                        sections = sections,
                        searchQuery = state.searchQuery,
                        onSearchQueryChanged = onSearchQueryChanged,
                        selectedTemplateId = state.selectedTemplate?.id,
                        selectedCategoryId = state.selectedCategoryId,
                        isSheetSettled = isSheetSettled,
                        onTemplateSelected = { template ->
                            onTemplateSelected(template)
                        },
                        onSuggestionSelected = onSuggestionSelected,
                        onCategorySelected = { category ->
                            onCategorySelected(category)
                        }
                    )

                    STEP_ACCOUNT -> QuickAddAccountStep(
                        accounts = state.accounts,
                        structureTypes = state.structureTypes,
                        selectedAccountId = state.selectedAccountId,
                        selectedToAccountId = state.selectedToAccountId,
                        entryMode = state.entryMode,
                        activeSide = state.activeAccountSide,
                        isStepActive = state.currentStep == STEP_ACCOUNT,
                        isSheetSettled = isSheetSettled,
                        onAccountTapped = onAccountTapped,
                        onToggleType = { onToggleType() },
                        onToggleTransfer = { onToggleTransfer() },
                        onSetFocus = onSetAccountFocus,
                        onSwapAccounts = onSwapAccounts
                    )

                    STEP_AMOUNT -> {
                        val selectedAccount =
                            state.accounts.find { it.id == state.selectedAccountId }
                        val selectedToAccount =
                            state.accounts.find { it.id == state.selectedToAccountId }
                        val selectedCategory = categories.find { it.id == state.selectedCategoryId }
                        QuickAddAmountStep(
                            amount = state.amount,
                            amountOverride = state.amountOverride,
                            isAmountOverridden = state.isAmountOverridden,
                            activeAmountSide = state.activeAmountSide,
                            template = state.selectedTemplate,
                            category = selectedCategory,
                            categories = categories,
                            account = selectedAccount,
                            toAccount = selectedToAccount,
                            accounts = state.accounts,
                            accountBalance = state.accountBalances[state.selectedAccountId]
                                ?: BigDecimal.ZERO,
                            toAccountBalance = state.accountBalances[state.selectedToAccountId]
                                ?: BigDecimal.ZERO,
                            entryMode = state.entryMode,
                            exchangeRates = state.exchangeRates,
                            baseCurrency = state.baseCurrency,
                            selectedCurrency = state.selectedCurrency,
                            selectedToCurrency = state.selectedToCurrency,
                            onCurrencySelected = onCurrencySelected,
                            onAmountChange = onAmountChange,
                            onSetAmountFocus = onSetAmountFocus,
                            onToggleAmountOverride = onToggleAmountOverride,
                            onSwapAccounts = onSwapAccounts,
                            onAccountSelected = onUpdateAccount,
                            onCategorySelected = onUpdateCategory,
                            onToggleEntryMode = onToggleType,
                            onToggleTransfer = onToggleTransfer,
                            onDone = {
                                if (!state.isSaving) {
                                    onSaveTransaction()
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
                    if (!state.isSaving) onSaveTransaction()
                } else if (canGoNext) {
                    goNext()
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddSheet_Template() {
    val mockState = QuickAddState(
        currentStep = STEP_TEMPLATE,
        accounts = listOf(
            Account("1", "Cash", "USD", AccountStructureType.CASH, AccountStructureType.CASH.colorHex, AccountStructureType.CASH.iconKey),
            Account("2", "Bank", "USD", AccountStructureType.BANK, AccountStructureType.BANK.colorHex, AccountStructureType.BANK.iconKey)
        ),
        selectedAccount = AccountRef.Unselected
    )
    val mockSections = listOf(
        QuickAddSection(
            "TEMPLATES",
            listOf(
                QuickAddResult.TemplateItem(
                    Template(
                        1,
                        "Coffee",
                        1,
                        BigDecimal("5.00"),
                        EntryMode.EXPENSE,
                        currency = "USD",
                        accountId = "1"
                    ),
                    "Coffee",
                    -123456
                )
            )
        ),
        QuickAddSection(
            "CATEGORIES",
            listOf(
                QuickAddResult.CategoryItem(
                    Category(
                        1,
                        "Food",
                        "Restaurant",
                        -16737025,
                        false,
                        null
                    )
                )
            )
        )
    )

    OssWalletTheme {
        QuickAddSheetInternal(
            state = mockState,
            sections = mockSections,
            categories = emptyList(),
            onDismiss = {},
            onReset = {},
            onSearchQueryChanged = {},
            onTemplateSelected = {},
            onSuggestionSelected = {},
            onCategorySelected = {},
            onAccountTapped = { _, _ -> },
            onUpdateAccount = { _, _ -> },
            onUpdateCategory = {},
            onToggleType = {},
            onToggleTransfer = {},
            onSwapAccounts = {},
            onAmountChange = {},
            onCurrencySelected = { _, _ -> },
            onSetAmountFocus = {},
            onSetAccountFocus = {},
            onToggleAmountOverride = {},
            onSaveTransaction = {},
            goToStep = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddSheet_Account() {
    val mockState = QuickAddState(
        currentStep = STEP_ACCOUNT,
        entryMode = EntryMode.TRANSFER,
        selectedAccount = AccountRef.Wallet("1"),
        accounts = listOf(
            Account("1", "Cash", "USD", AccountStructureType.CASH, AccountStructureType.CASH.colorHex, AccountStructureType.CASH.iconKey),
            Account("2", "Bank", "USD", AccountStructureType.BANK, AccountStructureType.BANK.colorHex, AccountStructureType.BANK.iconKey)
        ),
        structureTypes = AccountStructureType.entries
    )

    OssWalletTheme {
        QuickAddSheetInternal(
            state = mockState,
            sections = emptyList(),
            categories = emptyList(),
            onDismiss = {},
            onReset = {},
            onSearchQueryChanged = {},
            onTemplateSelected = {},
            onSuggestionSelected = {},
            onCategorySelected = {},
            onAccountTapped = { _, _ -> },
            onUpdateAccount = { _, _ -> },
            onUpdateCategory = {},
            onToggleType = {},
            onToggleTransfer = {},
            onSwapAccounts = {},
            onAmountChange = {},
            onCurrencySelected = { _, _ -> },
            onSetAmountFocus = {},
            onSetAccountFocus = {},
            onToggleAmountOverride = {},
            onSaveTransaction = {},
            goToStep = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickAddSheet_Amount() {
    val fromAccount = Account("1", "Cash", "USD", AccountStructureType.CASH, AccountStructureType.CASH.colorHex, AccountStructureType.CASH.iconKey)
    val toAccount =
        Account("2", "Bank", "USD", AccountStructureType.BANK, AccountStructureType.BANK.colorHex, AccountStructureType.BANK.iconKey)
    val mockState = QuickAddState(
        currentStep = STEP_AMOUNT,
        entryMode = EntryMode.TRANSFER,
        selectedAccount = AccountRef.Wallet("1"),
        selectedToAccount = AccountRef.Wallet("2"),
        amount = BigDecimal("100.00"),
        accounts = listOf(fromAccount, toAccount),
        accountBalances = mapOf("1" to BigDecimal("500.00"), "2" to BigDecimal("1200.00"))
    )

    OssWalletTheme {
        QuickAddSheetInternal(
            state = mockState,
            sections = emptyList(),
            categories = emptyList(),
            onDismiss = {},
            onReset = {},
            onSearchQueryChanged = {},
            onTemplateSelected = {},
            onSuggestionSelected = {},
            onCategorySelected = {},
            onAccountTapped = { _, _ -> },
            onUpdateAccount = { _, _ -> },
            onUpdateCategory = {},
            onToggleType = {},
            onToggleTransfer = {},
            onSwapAccounts = {},
            onAmountChange = {},
            onCurrencySelected = { _, _ -> },
            onSetAmountFocus = {},
            onSetAccountFocus = {},
            onToggleAmountOverride = {},
            onSaveTransaction = {},
            goToStep = {}
        )
    }
}

@Composable
private fun EditorialHeader(title: String, subtext: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = AppTextStyles.screenTitle.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = 18.sp
                ),
                color = AppColors.contentPrimary
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = AppTextStyles.sectionOverline,
                    color = AppColors.contentSecondary.copy(alpha = 0.85f)
                )
            }
        }
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
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
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
