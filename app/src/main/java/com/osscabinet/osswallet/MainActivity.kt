package com.osscabinet.ossWallet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.osscabinet.ossWallet.domain.repository.SettingsRepository
import com.osscabinet.ossWallet.ui.navigation.OssWalletNavGraph
import com.osscabinet.ossWallet.ui.security.AppLockManager
import com.osscabinet.ossWallet.ui.security.LockScreen
import com.osscabinet.ossWallet.ui.security.LockState
import com.osscabinet.ossWallet.ui.security.setSecure
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import android.content.Intent
import com.osscabinet.ossWallet.domain.model.settings.GlassSettings
import com.osscabinet.ossWallet.ui.theme.AppColors
import com.osscabinet.ossWallet.ui.theme.LocalGlassmorphismEnabled

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var keepSplashOnScreen = true

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var appLockManager: AppLockManager

    companion object {
        const val EXTRA_WIDGET_NAV = "WIDGET_NAV"
        /** URL-encoded JSON of a [com.osscabinet.ossWallet.domain.model.TransactionFilter] — same encoding [com.osscabinet.ossWallet.ui.navigation.NavigationExtensions.navigateToRecords] uses for [com.osscabinet.ossWallet.ui.navigation.TransactionListRoute.filterJson]. Only meaningful when EXTRA_WIDGET_NAV is "records". */
        const val EXTRA_WIDGET_FILTER = "WIDGET_FILTER"
        /** Transaction id (Long) to open for editing. Only meaningful when EXTRA_WIDGET_NAV is "transaction_entry" — set via the Recent Activities widget's per-row fill-in Intent. */
        const val EXTRA_WIDGET_TRANSACTION_ID = "WIDGET_TRANSACTION_ID"
        /** Id of the budget or account to open. Only meaningful when EXTRA_WIDGET_NAV is "budget_detail" or "account_detail". */
        const val EXTRA_WIDGET_ENTITY_ID = "WIDGET_ENTITY_ID"
    }

    private var pendingWidgetRoute by mutableStateOf<String?>(null)
    private var pendingWidgetFilter by mutableStateOf<String?>(null)
    private var pendingWidgetTransactionId by mutableStateOf<Long?>(null)
    private var pendingWidgetEntityId by mutableStateOf<String?>(null)
    private var pendingSharedText by mutableStateOf<String?>(null)

    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        return null
    }

    override fun onStart() {
        super.onStart()
        appLockManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        // A rotation/theme recreate also stops the activity; it must not count as leaving the app.
        if (!isChangingConfigurations) appLockManager.onAppBackgrounded()
    }

    // Every ActivityResultLauncher (file pickers, folder pickers) funnels through here.
    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        appLockManager.expectExternalReturn()
        @Suppress("DEPRECATION")
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetRoute = intent.getStringExtra(EXTRA_WIDGET_NAV)
        pendingWidgetFilter = intent.getStringExtra(EXTRA_WIDGET_FILTER)
        pendingWidgetTransactionId = intent.getLongExtra(EXTRA_WIDGET_TRANSACTION_ID, -1L).takeIf { it != -1L }
        pendingWidgetEntityId = intent.getStringExtra(EXTRA_WIDGET_ENTITY_ID)
        pendingSharedText = extractSharedText(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingWidgetRoute = intent?.getStringExtra(EXTRA_WIDGET_NAV)
        pendingWidgetFilter = intent?.getStringExtra(EXTRA_WIDGET_FILTER)
        pendingWidgetTransactionId = intent?.getLongExtra(EXTRA_WIDGET_TRANSACTION_ID, -1L)?.takeIf { it != -1L }
        pendingWidgetEntityId = intent?.getStringExtra(EXTRA_WIDGET_ENTITY_ID)
        pendingSharedText = extractSharedText(intent)

        setContent {
            val theme by settingsRepository.theme.collectAsStateWithLifecycle(initialValue = "System")
            val selectedFont by settingsRepository.selectedFont.collectAsStateWithLifecycle(initialValue = "MANROPE")
            val uiIdentity by settingsRepository.uiIdentity.collectAsStateWithLifecycle(initialValue = com.osscabinet.ossWallet.domain.model.settings.UiIdentity.DEFAULT.name)
            val dynamicAccent by settingsRepository.dynamicAccent.collectAsStateWithLifecycle(initialValue = false)
            val precision by settingsRepository.decimalPrecision.collectAsStateWithLifecycle(initialValue = 2)
            val numberingSystem by settingsRepository.numberingSystem.collectAsStateWithLifecycle(initialValue = "WESTERN")
            val use24Hour by settingsRepository.use24HourFormat.collectAsStateWithLifecycle(initialValue = false)
            val glassmorphismEnabled by settingsRepository.glassmorphismEnabled.collectAsStateWithLifecycle(initialValue = true)
            val contourBgVersion by settingsRepository.contourBgVersion.collectAsStateWithLifecycle(initialValue = 4)
            val contourLinesEnabled by settingsRepository.contourLinesEnabled.collectAsStateWithLifecycle(initialValue = true)
            val randomizeContourEachLoad by settingsRepository.randomizeContourEachLoad.collectAsStateWithLifecycle(initialValue = true)
            val glassSettings by settingsRepository.glassSettings.collectAsStateWithLifecycle(initialValue = GlassSettings())
            val uiDensityName by settingsRepository.uiDensity.collectAsStateWithLifecycle(initialValue = com.osscabinet.ossWallet.domain.model.settings.UiDensity.DEFAULT.name)
            val uiDensity = com.osscabinet.ossWallet.domain.model.settings.UiDensity.fromName(uiDensityName)
            val cardOpenAnimationName by settingsRepository.cardOpenAnimation.collectAsStateWithLifecycle(
                initialValue = com.osscabinet.ossWallet.domain.model.settings.CardOpenAnimation.DEFAULT.name
            )
            val cardOpenAnimation = com.osscabinet.ossWallet.domain.model.settings.CardOpenAnimation.fromName(cardOpenAnimationName)

            val lockState by appLockManager.state.collectAsStateWithLifecycle()
            val lockConfig by appLockManager.config.collectAsStateWithLifecycle()
            // Secure until the stored config proves otherwise, so a cold start never leaks a frame.
            val secure = lockConfig?.privacyScreenEnabled ?: true
            LaunchedEffect(secure) { window.setSecure(secure) }

            LaunchedEffect(precision, numberingSystem) {
                // ARC-05: atomic paired update — readers can never observe a half-applied
                // (new precision, old system) combination.
                com.osscabinet.ossWallet.domain.service.currency.CurrencyService.setFormatConfig(
                    precision = precision,
                    system = com.osscabinet.ossWallet.domain.service.currency.CurrencyService.FormattingSystem.fromString(numberingSystem)
                )
                com.osscabinet.ossWallet.domain.service.timeline.TimelineService.precision = precision
            }

            LaunchedEffect(use24Hour) {
                com.osscabinet.ossWallet.ui.util.TimeUtils.use24HourFormat = use24Hour
            }

            OssWalletTheme(
                theme = theme,
                fontName = selectedFont,
                identity = uiIdentity,
                dynamicAccent = dynamicAccent,
                glassSettings = glassSettings,
                uiDensity = uiDensity,
                cardOpenAnimation = cardOpenAnimation
            ) {
                CompositionLocalProvider(
                    LocalGlassmorphismEnabled provides glassmorphismEnabled,
                    com.osscabinet.ossWallet.ui.theme.LocalContourBgVersion provides contourBgVersion,
                    com.osscabinet.ossWallet.ui.theme.LocalContourLinesEnabled provides contourLinesEnabled,
                    com.osscabinet.ossWallet.ui.theme.LocalRandomizeContourEachLoad provides randomizeContourEachLoad
                ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.screenBackground
                ) {
                    val navController = rememberNavController()
                    val unlocked = lockState == LockState.Unlocked
                    // The nav graph normally dismisses the splash; it is not composed while locked.
                    LaunchedEffect(unlocked) { if (!unlocked) keepSplashOnScreen = false }

                    // Keep the graph out of composition while locked: no data is read or drawn
                    // behind the gate, and nothing underneath can receive touches or a11y focus.
                    if (unlocked) OssWalletNavGraph(
                        navController = navController,
                        onSplashConditionChanged = { keepSplashOnScreen = it },
                        pendingWidgetRoute = pendingWidgetRoute,
                        pendingWidgetFilter = pendingWidgetFilter,
                        pendingWidgetTransactionId = pendingWidgetTransactionId,
                        pendingWidgetEntityId = pendingWidgetEntityId,
                        pendingSharedText = pendingSharedText,
                        onWidgetRouteHandled = {
                            pendingWidgetRoute = null
                            pendingWidgetFilter = null
                            pendingWidgetTransactionId = null
                            pendingWidgetEntityId = null
                            pendingSharedText = null
                        }
                    )
                    if (!unlocked) {
                        LockScreen(
                            manager = appLockManager,
                            biometricEnabled = lockConfig?.biometricEnabled == true
                        )
                    }
                }
                } // CompositionLocalProvider
            }
        }
    }
}
