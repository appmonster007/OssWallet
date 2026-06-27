You are a Blind Hunter code reviewer. Review the following diff for bugs, logic errors, code quality issues, and security vulnerabilities. You have NO context about the project other than this diff.

## DIFF
```
diff --git a/app/src/main/java/com/osscabinet/osswallet/ui/components/cards/AeonExpressiveCard.kt b/app/src/main/java/com/osscabinet/osswallet/ui/components/cards/AeonExpressiveCard.kt
index 1fd333ee..1cc6dd03 100644
--- a/app/src/main/java/com/osscabinet/osswallet/ui/components/cards/AeonExpressiveCard.kt
+++ b/app/src/main/java/com/osscabinet/osswallet/ui/components/cards/AeonExpressiveCard.kt
@@ -38,6 +38,7 @@ import androidx.compose.material3.Text
 import androidx.compose.runtime.Composable
 import androidx.compose.runtime.CompositionLocalProvider
 import androidx.compose.runtime.DisposableEffect
+import androidx.compose.runtime.LaunchedEffect
 import androidx.compose.runtime.SideEffect
 import androidx.compose.runtime.derivedStateOf
 import androidx.compose.runtime.getValue
@@ -144,17 +145,15 @@ fun AeonExpressiveCard(
         label = "cardFlip"
     )
 
+    val isFrontVisible by remember { derivedStateOf { rotation <= 90f } }
+
     // Discoverability & Intent Feedback
-    SideEffect {
-        if (rotation == 180f || rotation == 0f) {
-            // No-op for settled state
-        } else if (rotation > 85f && rotation < 95f) {
+    LaunchedEffect(isFrontVisible) {
+        if (rotation > 0f && rotation < 180f) {
             haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
         }
     }
 
-    val isFrontVisible by remember { derivedStateOf { rotation <= 90f } }
-
     // timelineLabel allocates Calendar/SimpleDateFormat; cache so it doesn't run every recomposition.
     val effectiveSubtitle = remember(subtitle, filter?.timelineRange, timelineMode) {
         subtitle ?: if (!timelineMode) filter?.timelineRange?.let { timelineLabel(it) } else null
@@ -199,12 +198,16 @@ fun AeonExpressiveCard(
                 }
                 .background(
                     // Snap the surface prominent color at 90 degrees for clear mode switching
-                    if (rotation <= 90f) containerColor else AppColors.cardSurfaceProminent,
+                    if (isFrontVisible) containerColor else AppColors.cardSurfaceProminent,
                     cardShape
                 )
                 .then(borderModifier)
-                .animateContentSize(
-                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
+                .then(
+                    if (rotation == 0f || rotation == 180f) {
+                        Modifier.animateContentSize(
+                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
+                        )
+                    } else Modifier
                 )
                 .then(
                     if (onClick != null && interactionSource != null && !isFlipped) Modifier.clickable(
@@ -214,16 +217,9 @@ fun AeonExpressiveCard(
                     ) else Modifier,
                 ),
         ) {
-            // Dual-layered approach for zero-latency switching
-            // Front Side
-            Box(
-                modifier = Modifier
-                    .fillMaxWidth()
-                    .graphicsLayer {
-                        alpha = if (isFrontVisible) 1f else 0f
-                    }
-                    .then(if (!isFrontVisible) Modifier.height(0.dp) else Modifier)
-            ) {
+            // Single-layer approach for halved layout cost per frame
+            if (isFrontVisible) {
+                // Front Side
                 AeonGlassHost(modifier = Modifier.fillMaxWidth()) {
                     if (backgroundIcon != null) {
                         Icon(
@@ -310,22 +306,20 @@ fun AeonExpressiveCard(
                         content()
                     }
                 }
-            }
-
-            // Back Side
-            Box(
-                modifier = Modifier
-                    .fillMaxWidth()
-                    .graphicsLayer {
-                        rotationY = 180f
-                        alpha = if (!isFrontVisible) 1f else 0f
+            } else {
+                // Back Side
+                Box(
+                    modifier = Modifier
+                        .fillMaxWidth()
+                        .graphicsLayer {
+                            rotationY = 180f
+                        }
+                        .background(AppColors.cardSurfaceProminent)
+                ) {
+                    displaySettings?.invoke {
+                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
+                        isFlipped = false
                     }
-                    .then(if (isFrontVisible) Modifier.height(0.dp) else Modifier)
-                    .background(AppColors.cardSurfaceProminent)
-            ) {
-                displaySettings?.invoke {
-                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
-                    isFlipped = false
                 }
             }
         }
diff --git a/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardScreen.kt b/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardScreen.kt
index 70094c8d..d9b88556 100644
--- a/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardScreen.kt
+++ b/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardScreen.kt
@@ -192,10 +192,8 @@ fun DashboardScreen(
     vms: SharedNavViewModels
 ) {
     val dashboardViewModel = vms.dashboardViewModel
-    val transactionViewModel = vms.transactionViewModel
-    val settingsViewModel = vms.settingsViewModel
-    val templateViewModel = vms.templateViewModel
-    val cardComputerRegistry = vms.dashboardViewModel.cardComputerRegistry
+    val coroutineScope = rememberCoroutineScope()
+    val cardComputerRegistry = dashboardViewModel.cardComputerRegistry
 
     // Apply initial view focus when screen opens with context
     LaunchedEffect(config.viewContextJson) {
@@ -204,94 +202,82 @@ fun DashboardScreen(
                 val decodedContext =
                     URLDecoder.decode(contextString, StandardCharsets.UTF_8.toString())
                 val context = Json.decodeFromString<DashboardViewContext>(decodedContext)
-                dashboardViewModel.initializeDashboardView(context)
+                dashboardViewModel.onIntent(DashboardIntent.InitializeView(context))
             } catch (e: Exception) {
-                // If context is invalid, use default
-                dashboardViewModel.initializeDashboardView(DashboardViewContext.None)
+                dashboardViewModel.onIntent(DashboardIntent.InitializeView(DashboardViewContext.None))
             }
         } ?: run {
-            // If no context provided, ensure view focus is cleared
-            dashboardViewModel.clearViewFocus()
+            dashboardViewModel.onIntent(DashboardIntent.ClearViewFocus)
         }
     }
 
     // Clear view focus when leaving the screen
     DisposableEffect(Unit) {
         onDispose {
-            dashboardViewModel.clearViewFocus()
+            dashboardViewModel.onIntent(DashboardIntent.ClearViewFocus)
         }
     }
 
     val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
-    val initialViewContext by dashboardViewModel.initialViewContext.collectAsStateWithLifecycle()
-    val cardsConfig by dashboardViewModel.cardsConfig.collectAsStateWithLifecycle()
 
-    val displayPrefs by settingsViewModel.dashboardDisplayPrefs.collectAsStateWithLifecycle()
+    val successState = dashboardUiState as? DashboardUiState.Success
 
-    val allTemplates by templateViewModel.templates.collectAsStateWithLifecycle()
-    val allCategories by transactionViewModel.categories.collectAsStateWithLifecycle()
-    val lastUndoAction by dashboardViewModel.lastUndoAction.collectAsStateWithLifecycle()
-
-    val savedFilters by transactionViewModel.savedFilters.collectAsStateWithLifecycle()
-    val revealedCardIds by transactionViewModel.revealedCardIdsList.collectAsStateWithLifecycle()
-    val transactionSnackbarMessage by transactionViewModel.snackbarMessage.collectAsStateWithLifecycle()
-    val scrollToId by transactionViewModel.scrollToId.collectAsStateWithLifecycle()
-    val highlightedId by transactionViewModel.highlightedId.collectAsStateWithLifecycle()
-    val linkedPopupTxs by transactionViewModel.linkedTransactionsPopup.collectAsStateWithLifecycle()
-
-    val isTopLevel = remember(initialViewContext, config.isTopLevel) {
+    val isTopLevel = remember(successState?.initialViewContext, config.isTopLevel) {
+        val initialViewContext = successState?.initialViewContext ?: DashboardViewContext.None
         config.isTopLevel || initialViewContext == DashboardViewContext.None
     }
 
     DashboardScreenLayout(
         isTopLevel = isTopLevel,
-        initialViewContext = initialViewContext,
+        initialViewContext = successState?.initialViewContext ?: DashboardViewContext.None,
         uiState = dashboardUiState,
-        cardsConfig = cardsConfig,
-        currentProfileName = displayPrefs.currentProfileName,
-        cardAnimationEnabled = displayPrefs.cardAnimationEnabled,
-        expandableFabEnabled = displayPrefs.expandableFabEnabled,
-        allTemplates = allTemplates,
-        allCategories = allCategories,
-        lastUndoAction = lastUndoAction,
-        savedFilters = savedFilters,
-        revealedCardIds = revealedCardIds,
-        transactionSnackbarMessage = transactionSnackbarMessage,
-        scrollToId = scrollToId,
-        highlightedId = highlightedId,
-        recentActivitiesBreakdown = displayPrefs.recentActivitiesBreakdown,
+        cardsConfig = successState?.cardsConfig ?: DashboardCardsConfig(),
+        currentProfileName = successState?.displayPrefs?.currentProfileName,
+        cardAnimationEnabled = successState?.displayPrefs?.cardAnimationEnabled ?: true,
+        expandableFabEnabled = successState?.displayPrefs?.expandableFabEnabled ?: true,
+        allTemplates = successState?.allTemplates ?: emptyList(),
+        allCategories = successState?.allCategories ?: emptyList(),
+        lastUndoAction = successState?.lastUndoAction,
+        savedFilters = successState?.savedFilters ?: emptyList(),
+        revealedCardIds = successState?.revealedCardIds?.toList() ?: emptyList(),
+        transactionSnackbarMessage = successState?.transactionSnackbarMessage,
+        scrollToId = successState?.scrollToId,
+        highlightedId = successState?.highlightedId,
+        recentActivitiesBreakdown = successState?.displayPrefs?.recentActivitiesBreakdown ?: true,
         actions = actions,
-        onResetView = dashboardViewModel::resetToInitialView,
-        onUndoAction = dashboardViewModel::undoLastPlannedPaymentAction,
-        onClearUndo = dashboardViewModel::clearUndoAction,
-        onClearTransactionSnackbar = transactionViewModel::clearSnackbar,
-        onClearScrollTo = transactionViewModel::clearScrollTo,
+        onResetView = { dashboardViewModel.onIntent(DashboardIntent.ResetToInitialView) },
+        onUndoAction = { dashboardViewModel.onIntent(DashboardIntent.UndoLastPlannedPaymentAction) },
+        onClearUndo = { dashboardViewModel.onIntent(DashboardIntent.ClearUndoAction) },
+        onClearTransactionSnackbar = { dashboardViewModel.onIntent(DashboardIntent.ClearSnackbar) },
+        onClearScrollTo = { dashboardViewModel.onIntent(DashboardIntent.ClearScrollTo) },
         onAddCard = dashboardViewModel::addCard,
         onReorderCards = dashboardViewModel::reorderCards,
         onRemoveCard = dashboardViewModel::removeCard,
-        onQuickConfirm = dashboardViewModel::quickConfirmById,
-        onQuickSkip = dashboardViewModel::quickSkipById,
-        onSearchTransactions = transactionViewModel::searchTransactions,
-        onItemExpanded = transactionViewModel::onItemExpanded,
-        onItemCollapsed = transactionViewModel::onItemCollapsed,
-        onStatusChange = transactionViewModel::quickUpdateStatus,
-        onDeleteTransaction = transactionViewModel::quickDeleteTransaction,
-        onFullSwipeRight = transactionViewModel::toggleStatusClearedUncleared,
-        onJumpTransaction = { id, availableIds ->
-            val finalAvailableIds = availableIds ?: (dashboardUiState as? DashboardUiState.Success)
-                ?.recentActivitiesState?.getAllTransactionIds()
-            transactionViewModel.handleJumpRequest(id, finalAvailableIds)
+        onQuickConfirm = { id, date -> dashboardViewModel.onIntent(DashboardIntent.QuickConfirmPlannedPayment(id, date)) },
+        onQuickSkip = { id, date -> dashboardViewModel.onIntent(DashboardIntent.QuickSkipPlannedPayment(id, date)) },
+        onSearchTransactions = { query -> dashboardViewModel.searchTransactions(query) },
+        onItemExpanded = { dashboardViewModel.onIntent(DashboardIntent.OnCardExpanded(it)) },
+        onItemCollapsed = { dashboardViewModel.onIntent(DashboardIntent.OnCardCollapsed(it)) },
+        onStatusChange = { id, status -> 
+            coroutineScope.launch { 
+                dashboardViewModel.transactionManager.updateStatus(id, status, successState?.profileId ?: 1) 
+            }
+        },
+        onDeleteTransaction = { id ->
+            coroutineScope.launch {
+                dashboardViewModel.transactionManager.deleteTransaction(id, successState?.profileId ?: 1)
+            }
         },
-        onPerformJump = { id, availableIds ->
-            val finalAvailableIds = availableIds ?: (dashboardUiState as? DashboardUiState.Success)
-                ?.recentActivitiesState?.getAllTransactionIds()
-            transactionViewModel.performJump(id, finalAvailableIds)
+        onFullSwipeRight = { id -> dashboardViewModel.onIntent(DashboardIntent.ToggleStatusClearedUncleared(id)) },
+        onJumpTransaction = { id, _ -> dashboardViewModel.onIntent(DashboardIntent.HandleJumpRequest(id)) },
+        onPerformJump = { id, _ -> dashboardViewModel.onIntent(DashboardIntent.PerformJump(id)) },
+        onCloseLinkedPopup = { dashboardViewModel.onIntent(DashboardIntent.CloseLinkedPopup) },
+        linkedPopupTxs = successState?.linkedPopupTxs,
+        onAddTag = { name, color ->
+            coroutineScope.launch { dashboardViewModel.tagRepository.insertTag(Tag(name = name, colorHex = color)) }
         },
-        onCloseLinkedPopup = transactionViewModel::closeLinkedPopup,
-        linkedPopupTxs = linkedPopupTxs,
-        onAddTag = transactionViewModel::addTag,
-        onConfirmPlanned = transactionViewModel::confirmPlannedPayment,
-        onCancelPlanned = transactionViewModel::cancelPlannedPayment,
+        onConfirmPlanned = { id, date -> dashboardViewModel.onIntent(DashboardIntent.QuickConfirmPlannedPayment(id, date)) },
+        onCancelPlanned = { id, date -> dashboardViewModel.onIntent(DashboardIntent.QuickSkipPlannedPayment(id, date)) },
         cardComputerRegistry = cardComputerRegistry,
         cardUpdateHandler = dashboardViewModel
     )
diff --git a/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardViewModel.kt b/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardViewModel.kt
index 70023c37..056961bd 100644
--- a/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardViewModel.kt
+++ b/app/src/main/java/com/osscabinet/osswallet/ui/screens/dashboard/DashboardViewModel.kt
@@ -1,6 +1,8 @@
 package com.osscabinet.ossWallet.ui.screens.dashboard
 
+import androidx.compose.runtime.Immutable
 import androidx.lifecycle.viewModelScope
+import com.osscabinet.ossWallet.core.util.DispatcherProvider
 import com.osscabinet.ossWallet.domain.card.CardComputerRegistry
 import com.osscabinet.ossWallet.domain.card.CardInput
 import com.osscabinet.ossWallet.domain.card.CardRenderData
@@ -42,8 +44,20 @@ import com.osscabinet.ossWallet.domain.usecase.TransactionUseCases
 import com.osscabinet.ossWallet.ui.navigation.DashboardViewContext
 import com.osscabinet.ossWallet.ui.screens.plannedpayments.UndoAction
 import com.osscabinet.ossWallet.ui.mapper.TransactionUiMapper
+import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListItem
 import com.osscabinet.ossWallet.ui.screens.transactions.model.TransactionListUiState
 import com.osscabinet.ossWallet.ui.util.BaseViewModel
+import com.osscabinet.ossWallet.domain.model.Template
+import com.osscabinet.ossWallet.domain.model.Filter
+import com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType
+import com.osscabinet.ossWallet.domain.model.settings.TransactionSectionSettings
+import com.osscabinet.ossWallet.domain.repository.FilterRepository
+import com.osscabinet.ossWallet.domain.repository.ProfileRepository
+import com.osscabinet.ossWallet.domain.repository.TemplateRepository
+import com.osscabinet.ossWallet.ui.screens.settings.SettingsViewModel.DashboardDisplayPrefs
+import kotlinx.coroutines.delay
+import kotlinx.coroutines.flow.emitAll
+import kotlinx.coroutines.flow.flowOf
 import dagger.hilt.android.lifecycle.HiltViewModel
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.flow.Flow
@@ -88,8 +102,12 @@ class DashboardViewModel @Inject constructor(
     private val plannedPaymentRepository: PlannedPaymentRepository,
     private val budgetRepository: BudgetRepository,
     private val loanRepository: LoanRepository,
-    private val tagRepository: TagRepository,
+    internal val tagRepository: TagRepository,
+    private val templateRepository: TemplateRepository,
+    private val filterRepository: FilterRepository,
+    private val profileRepository: ProfileRepository,
     private val cardRenderCache: CardRenderCache,
+    private val dispatchers: DispatcherProvider,
 ) : BaseViewModel(settingsRepository), CardUpdateHandler {
 
     override val cardUpdateScope = viewModelScope
@@ -113,15 +131,65 @@ class DashboardViewModel @Inject constructor(
     private val _lastUndoAction = MutableStateFlow<UndoAction?>(null)
     val lastUndoAction: StateFlow<UndoAction?> = _lastUndoAction.asStateFlow()
 
-    fun initializeDashboardView(viewContext: DashboardViewContext) {
+    private val _revealedCardIds = MutableStateFlow<Set<String>>(emptySet())
+    private val _snackbarMessage = MutableStateFlow<String?>(null)
+    private val _scrollToId = MutableStateFlow<String?>(null)
+    private val _highlightedId = MutableStateFlow<String?>(null)
+    private val _linkedPopupTxs = MutableStateFlow<List<TransactionListItem>?>(null)
+
+    fun onIntent(intent: DashboardIntent) {
+        when (intent) {
+            is DashboardIntent.InitializeView -> _initialViewContext.value = intent.viewContext
+            DashboardIntent.ResetToInitialView -> _initialViewContext.value = _initialViewContext.value
+            DashboardIntent.ClearViewFocus -> _viewFocus.value = ViewFocus()
+            is DashboardIntent.QuickConfirmPlannedPayment -> quickConfirmById(intent.plannedPaymentId, intent.scheduledDate)
+            is DashboardIntent.QuickSkipPlannedPayment -> quickSkipById(intent.plannedPaymentId, intent.scheduledDate)
+            DashboardIntent.UndoLastPlannedPaymentAction -> undoLastPlannedPaymentAction()
+            DashboardIntent.ClearUndoAction -> _lastUndoAction.value = null
+            is DashboardIntent.CardBecameVisible -> { /* on-demand card computation — deferred to Story 2.3 pipeline refactor */ }
+            
+            // New UI Intents
+            is DashboardIntent.OnCardExpanded -> _revealedCardIds.update { it + intent.cardId }
+            is DashboardIntent.OnCardCollapsed -> _revealedCardIds.update { it - intent.cardId }
+            DashboardIntent.ClearSnackbar -> _snackbarMessage.value = null
+            DashboardIntent.ClearScrollTo -> _scrollToId.value = null
+            DashboardIntent.CloseLinkedPopup -> _linkedPopupTxs.value = null
+            is DashboardIntent.HandleJumpRequest -> handleJumpRequest(intent.transactionId)
+            is DashboardIntent.PerformJump -> performJump(intent.transactionId)
+            is DashboardIntent.ToggleStatusClearedUncleared -> toggleStatusClearedUncleared(intent.transactionId)
+        }
+    }
+
+    private fun toggleStatusClearedUncleared(id: Long) {
+        viewModelScope.launch {
+            val profileId = settingsRepository.currentProfileId.first()
+            val tx = transactionUseCases.getTransactionById(id, profileId) ?: return@launch
+            val newStatus = if (tx.status == com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED) 
+                com.osscabinet.ossWallet.domain.model.TransactionStatus.UNCLEARED 
+            else 
+                com.osscabinet.ossWallet.domain.model.TransactionStatus.CLEARED
+            transactionManager.updateStatus(id, newStatus, profileId)
+        }
+    }
+
+    fun searchTransactions(query: String): Flow<List<Transaction>> = flow {
+        if (query.trim().length < 2) {
+            emit(emptyList())
+            return@flow
+        }
+        val profileId = settingsRepository.currentProfileId.first()
+        emitAll(transactionUseCases.searchTransactions(profileId, query))
+    }
+
+    private fun initializeDashboardView(viewContext: DashboardViewContext) {
         _initialViewContext.value = viewContext
     }
 
-    fun resetToInitialView() {
+    private fun resetToInitialView() {
         initializeDashboardView(_initialViewContext.value)
     }
 
-    fun clearViewFocus() {
+    private fun clearViewFocus() {
         _viewFocus.value = ViewFocus()
     }
 
@@ -165,8 +233,7 @@ class DashboardViewModel @Inject constructor(
     @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
     val uiState: StateFlow<DashboardUiState> = settingsRepository.currentProfileId
         .flatMapLatest { profileId: Int ->
-            // 1. Accounting Pipeline: Driven by financial changes.
-            // Computes balances and groups only when accounting data actually changes.
+            // 1. Accounting Pipeline
             val structuralStateFlow = combine(
                 accountRepository.getAllAccounts(profileId),
                 transactionUseCases.getTransactions(profileId),
@@ -190,26 +257,26 @@ class DashboardViewModel @Inject constructor(
                         exchangeRates = snapshot.currency.rates,
                         limit = DASHBOARD_TRANSACTION_FEED_LIMIT
                     ).map { result: Result<List<com.osscabinet.ossWallet.domain.model.TransactionWithBalance>> ->
-                        val transactionsWithBalance = result.getOrNull() ?: emptyList<com.osscabinet.ossWallet.domain.model.TransactionWithBalance>()
+                        val transactionsWithBalance = result.getOrNull() ?: emptyList()
                         
                         val visibleGroups = TimelineService.getGroupedData(
                             transactions = transactionsWithBalance.take(20),
                             absoluteHistory = transactionsWithBalance,
-                            groupingType = com.osscabinet.ossWallet.domain.model.settings.TransactionGroupingType.BY_DAY,
+                            groupingType = TransactionGroupingType.BY_DAY,
                             accounts = snapshot.accounts,
                             categories = snapshot.categories,
                             allTags = emptyList(),
                             baseCurrency = snapshot.currency.baseCode,
                             exchangeRates = snapshot.currency.rates,
                             linkedTransactionIds = snapshot.links,
-                            initialBalances = emptyMap() // Handled by anchored history
+                            initialBalances = emptyMap()
                         )
                         StructuralState(snapshot, CategoryHierarchyCache.build(snapshot.categories), visibleGroups)
-                    }.flowOn(Dispatchers.Default)
+                    }.flowOn(dispatchers.default)
                 }
             }
 
-            // 2. Metadata Pipeline...
+            // 2. Metadata Pipeline
             val metadataFlow = combine(
                 plannedPaymentRepository.getActivePlannedPayments(profileId),
                 budgetRepository.getAllBudgets(profileId),
@@ -220,82 +287,154 @@ class DashboardViewModel @Inject constructor(
                 MetadataSnapshot(pp, b, l, lp.groupBy { it.loanId }, t)
             }.distinctUntilChanged()
 
-            // 3. UI State Pipeline: Final integration.
-            combine(structuralStateFlow, metadataFlow, cardsConfig) { structuralState, metadata, config ->
-                Triple(structuralState, metadata, config)
-            }.flatMapLatest { (structuralState: StructuralState?, metadata: MetadataSnapshot, config: DashboardCardsConfig) ->
-                channelFlow<DashboardUiState> {
-                    if (structuralState == null) {
-                        send(DashboardUiState.Loading)
-                        return@channelFlow
+            // 3. App State Pipeline (Consolidation)
+            val appStateFlow = combine(
+                combine(
+                    _initialViewContext,
+                    cardsConfig,
+                    combine(
+                        settingsRepository.cardAnimationEnabled,
+                        settingsRepository.expandableFabEnabled,
+                        settingsRepository.recentActivitiesBreakdown,
+                        profileRepository.getAllProfiles()
+                    ) { anim, fab, breakdown, profiles ->
+                        DashboardDisplayPrefs(
+                            currentProfileName = profiles.find { it.id == profileId }?.name,
+                            cardAnimationEnabled = anim,
+                            expandableFabEnabled = fab,
+                            recentActivitiesBreakdown = breakdown
+                        )
                     }
-                    val snapshot = structuralState.snapshot
-                    val hierarchy = structuralState.hierarchy
-                    val visibleGroups = structuralState.visibleGroups
-
-                    val baseInput = CardInput(
-                        profileId = profileId,
-                        transactions = snapshot.transactions,
-                        allProfileTransactions = snapshot.transactions,
-                        accounts = snapshot.accounts,
-                        categories = snapshot.categories,
-                        tags = metadata.tags,
-                        exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(snapshot.currency.baseCode, snapshot.currency.rates),
-                        targetCurrency = snapshot.currency.baseCode,
-                        plannedPayments = metadata.plannedPayments,
-                        budgets = metadata.budgets,
-                        loans = metadata.loans,
-                        loanPayments = metadata.loanPayments,
-                        filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true)),
-                        screenContext = ScreenContext.DASHBOARD,
-                        commonFilterEnabled = false,
-                        categoryHierarchy = hierarchy
-                    )
-
-                    fun buildSuccess(cardMap: Map<String, CardRenderData>, computing: Boolean) = DashboardUiState.Success(
-                        profileId = profileId,
-                        accounts = snapshot.accounts.filter { !it.isArchived },
-                        allAccounts = snapshot.accounts,
-                        cardDataMap = cardMap,
-                        isCardsComputing = computing,
-                        allTransactions = snapshot.transactions,
-                        recentActivitiesState = TransactionUiMapper.mapToUiState(
-                            domainState = TransactionListState(visibleGroups, visibleGroups.isEmpty()),
-                            baseCurrency = snapshot.currency.baseCode
-                        ),
-                        allCategories = snapshot.categories,
-                        allTags = metadata.tags,
-                        baseCurrency = snapshot.currency.baseCode,
-                        exchangeRates = snapshot.currency.rates,
-                        plannedPayments = metadata.plannedPayments,
-                        budgets = metadata.budgets,
-                        loans = metadata.loans,
-                        loanPayments = metadata.loanPayments
+                ) { initialCtx, config, prefs ->
+                    Triple(initialCtx, config, prefs)
+                },
+                combine(
+                    templateRepository.getAllTemplates(profileId),
+                    filterRepository.getAllFilters(),
+                    _lastUndoAction
+                ) { templates, filters, undo ->
+                    Triple(templates, filters, undo)
+                },
+                combine(
+                    _revealedCardIds,
+                    _snackbarMessage,
+                    _scrollToId,
+                    _highlightedId,
+                    _linkedPopupTxs
+                ) { revealed, snackbar, scroll, highlighted, popup ->
+                    Pair(revealed, Pair(snackbar, Triple(scroll, highlighted, popup)))
+                }
+            ) { part1, part2, part3 ->
+                DashboardAppState(
+                    initialViewContext = part1.first,
+                    cardsConfig = part1.second,
+                    displayPrefs = part1.third,
+                    allTemplates = part2.first,
+                    savedFilters = part2.second,
+                    lastUndoAction = part2.third,
+                    revealedCardIds = part3.first,
+                    transactionSnackbarMessage = part3.second.first,
+                    scrollToId = part3.second.second.first,
+                    highlightedId = part3.second.second.second,
+                    linkedPopupTxs = part3.second.second.third
+                )
+            }
+
+            // 4. UI State Pipeline
+            combine(structuralStateFlow, metadataFlow, appStateFlow) { s, m, a ->
+                if (s == null) return@combine DashboardUiState.Loading
+
+                val snapshot = s.snapshot
+                val hierarchy = s.hierarchy
+                
+                // Moved CardInput construction into ViewModel (Story 2.3)
+                val cardInput = CardInput(
+                    profileId = profileId,
+                    transactions = snapshot.transactions,
+                    allProfileTransactions = snapshot.transactions,
+                    accounts = snapshot.accounts,
+                    categories = snapshot.categories,
+                    tags = m.tags,
+                    exchangeRates = com.osscabinet.ossWallet.domain.service.currency.ExchangeRateSnapshot(snapshot.currency.baseCode, snapshot.currency.rates),
+                    targetCurrency = snapshot.currency.baseCode,
+                    plannedPayments = m.plannedPayments,
+                    budgets = m.budgets,
+                    loans = m.loans,
+                    loanPayments = m.loanPayments,
+                    filter = TransactionFilter(timelineRange = TimelineRange.AllTime(excludeFuture = true)),
+                    screenContext = ScreenContext.DASHBOARD,
+                    commonFilterEnabled = false,
+                    categoryHierarchy = hierarchy
+                )
+
+                val successState = DashboardUiState.Success(
+                    profileId = profileId,
+                    accounts = snapshot.accounts.filter { !it.isArchived },
+                    allAccounts = snapshot.accounts,
+                    cardDataMap = emptyMap(),
+                    isCardsComputing = true,
+                    allTransactions = snapshot.transactions,
+                    recentActivitiesState = TransactionUiMapper.mapToUiState(
+                        domainState = TransactionListState(s.visibleGroups, s.visibleGroups.isEmpty()),
+                        baseCurrency = snapshot.currency.baseCode
+                    ),
+                    allCategories = snapshot.categories,
+                    allTags = m.tags,
+                    baseCurrency = snapshot.currency.baseCode,
+                    exchangeRates = snapshot.currency.rates,
+                    plannedPayments = m.plannedPayments,
+                    budgets = m.budgets,
+                    loans = m.loans,
+                    loanPayments = m.loanPayments,
+                    
+                    // Consolidated fields
+                    initialViewContext = a.initialViewContext,
+                    cardsConfig = a.cardsConfig,
+                    displayPrefs = a.displayPrefs,
+                    allTemplates = a.allTemplates,
+                    savedFilters = a.savedFilters,
+                    lastUndoAction = a.lastUndoAction,
+                    revealedCardIds = a.revealedCardIds,
+                    transactionSnackbarMessage = a.transactionSnackbarMessage,
+                    scrollToId = a.scrollToId,
+                    highlightedId = a.highlightedId,
+                    linkedPopupTxs = a.linkedPopupTxs,
+                    cardInput = cardInput
+                )
+                successState
+            }.flatMapLatest { state ->
+                if (state is DashboardUiState.Loading) return@flatMapLatest flowOf(state)
+                val successState = state as DashboardUiState.Success
+                
+                channelFlow<DashboardUiState> {
+                    val configs = successState.cardsConfig.cards
+                    val cachedCards = cardRenderCache[profileId, successState.baseCurrency]
                     val cardDataFlow = MutableStateFlow(cachedCards ?: emptyMap())
-                    val completedCount = AtomicInteger(0)
-                    val configs = config.cards
+                    val completedCount = java.util.concurrent.atomic.AtomicInteger(0)
 
                     launch {
-                        cardDataFlow.collect { send(buildSuccess(it, completedCount.get() < configs.size)) }
+                        cardDataFlow.collect { currentCards ->
+                            send(successState.copy(
+                                cardDataMap = currentCards,
+                                isCardsComputing = completedCount.get() < configs.size
+                            ))
+                        }
                     }
 
-                    configs.forEach { cardConfig ->
-                        launch(Dispatchers.Default) {
-                            val result = runCatching { cardComputerRegistry.compute(cardConfig.cardType, baseInput, cardConfig) }
-                            val data = result.getOrElse { e -> CardRenderData.Error(e.message) }
-                            cardDataFlow.update { it + (cardConfig.cardId to data) }
-                            
-                            if (completedCount.incrementAndGet() == configs.size) {
-                                cardRenderCache[profileId, snapshot.currency.baseCode] = cardDataFlow.value
-                                    .filterValues { it !is CardRenderData.Error }
+                    if (configs.isEmpty()) {
+                        completedCount.set(0)
+                        cardDataFlow.update { it } 
+                    } else {
+                        configs.forEach { cardConfig ->
+                            launch(dispatchers.default) {
+                                val result = runCatching { cardComputerRegistry.compute(cardConfig.cardType, successState.cardInput!!, cardConfig) }
+                                val data = result.getOrElse { e -> CardRenderData.Error(e.message) }
+                                cardDataFlow.update { it + (cardConfig.cardId to data) }
+                                
+                                if (completedCount.incrementAndGet() == configs.size) {
+                                    cardRenderCache[profileId, successState.baseCurrency] = cardDataFlow.value
+                                        .filterValues { it !is CardRenderData.Error }
+                                }
                             }
                         }
                     }
@@ -309,7 +448,7 @@ class DashboardViewModel @Inject constructor(
             initialValue = DashboardUiState.Loading
         )
 
-    fun quickConfirmById(id: Long, scheduledDate: Long) {
+    private fun quickConfirmById(id: Long, scheduledDate: Long) {
         val state = uiState.value as? DashboardUiState.Success ?: return
         val payment = state.plannedPayments.find { it.id == id } ?: return
         viewModelScope.launch {
@@ -344,7 +483,7 @@ class DashboardViewModel @Inject constructor(
         }
     }
 
-    fun quickSkipById(id: Long, scheduledDate: Long) {
+    private fun quickSkipById(id: Long, scheduledDate: Long) {
         val state = uiState.value as? DashboardUiState.Success ?: return
         val payment = state.plannedPayments.find { it.id == id } ?: return
         viewModelScope.launch {
@@ -356,7 +495,7 @@ class DashboardViewModel @Inject constructor(
         }
     }
 
-    fun undoLastPlannedPaymentAction() {
+    private fun undoLastPlannedPaymentAction() {
         val action = _lastUndoAction.value ?: return
         _lastUndoAction.value = null
         viewModelScope.launch {
@@ -381,7 +520,53 @@ class DashboardViewModel @Inject constructor(
         }
     }
 
-    fun clearUndoAction() { _lastUndoAction.value = null }
+    private fun clearUndoAction() { _lastUndoAction.value = null }
+
+    private fun handleJumpRequest(id: Long) {
+        viewModelScope.launch {
+            val profileId = settingsRepository.currentProfileId.first()
+            val links = transactionUseCases.getLinkedTransactionIds(id).first()
+            if (links.isEmpty()) {
+                _snackbarMessage.value = "No linked transactions found"
+                return@launch
+            }
+            
+            if (links.size == 1) {
+                performJump(links[0])
+            } else {
+                val baseCurr = computationRepository.selectedCurrency.first()
+                val rates = computationRepository.exchangeRates.first()
+                val enriched = transactionUseCases.getLinkedTransactions(id).first()
+                    .let { TimelineService.enrichTransactions(it, accountRepository.getAllAccounts(profileId).first(), categoryRepository.categories.first(), tagRepository.getAllTags().first(), baseCurr, rates) }
+                
+                _linkedPopupTxs.value = enriched.map { TransactionUiMapper.mapRecord(it, baseCurr) }
+            }
+        }
+    }
+
+    private fun performJump(id: Long) {
+        viewModelScope.launch {
+            _scrollToId.value = "tx_$id"
+            _highlightedId.value = "tx_$id"
+            _linkedPopupTxs.value = null
+            delay(1000)
+            if (_highlightedId.value == "tx_$id") _highlightedId.value = null
+        }
+    }
+
+    private data class DashboardAppState(
+        val initialViewContext: DashboardViewContext,
+        val cardsConfig: DashboardCardsConfig,
+        val displayPrefs: DashboardDisplayPrefs,
+        val allTemplates: List<Template>,
+        val savedFilters: List<Filter>,
+        val lastUndoAction: UndoAction?,
+        val revealedCardIds: Set<String>,
+        val transactionSnackbarMessage: String?,
+        val scrollToId: String?,
+        val highlightedId: String?,
+        val linkedPopupTxs: List<TransactionListItem>?
+    )
 
     private companion object {
         const val DASHBOARD_TRANSACTION_FEED_LIMIT = 100
@@ -390,6 +575,8 @@ class DashboardViewModel @Inject constructor(
 
 sealed class DashboardUiState {
     object Loading : DashboardUiState()
+
+    @Immutable
     data class Success(
         val profileId: Int,
         val accounts: List<Account>,
@@ -405,8 +592,23 @@ sealed class DashboardUiState {
         val plannedPayments: List<PlannedPayment> = emptyList(),
         val budgets: List<Budget> = emptyList(),
         val loans: List<Loan> = emptyList(),
-        val loanPayments: Map<Long, List<LoanPayment>> = emptyMap()
+        val loanPayments: Map<Long, List<LoanPayment>> = emptyMap(),
+        
+        // Consolidated Dashboard state
+        val initialViewContext: DashboardViewContext = DashboardViewContext.None,
+        val cardsConfig: DashboardCardsConfig = DashboardCardsConfig(),
+        val displayPrefs: DashboardDisplayPrefs? = null,
+        val allTemplates: List<Template> = emptyList(),
+        val savedFilters: List<Filter> = emptyList(),
+        val lastUndoAction: UndoAction? = null,
+        val revealedCardIds: Set<String> = emptySet(),
+        val transactionSnackbarMessage: String? = null,
+        val scrollToId: String? = null,
+        val highlightedId: String? = null,
+        val linkedPopupTxs: List<TransactionListItem>? = null,
+        val cardInput: CardInput? = null
     ) : DashboardUiState()
 
+    @Immutable
     data class Error(val message: String) : DashboardUiState()
 }
```