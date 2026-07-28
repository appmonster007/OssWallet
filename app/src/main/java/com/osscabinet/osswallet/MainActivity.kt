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
import com.osscabinet.ossWallet.ui.navigation.AnalyticsRoute
import com.osscabinet.ossWallet.ui.navigation.DashboardRoute
import com.osscabinet.ossWallet.ui.navigation.ExchangeRatesRoute
import com.osscabinet.ossWallet.ui.navigation.OssWalletNavGraph
import com.osscabinet.ossWallet.ui.navigation.TransactionListRoute
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

    companion object {
        const val EXTRA_WIDGET_NAV = "WIDGET_NAV"
    }

    private var pendingWidgetRoute by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetRoute = intent.getStringExtra(EXTRA_WIDGET_NAV)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingWidgetRoute = intent?.getStringExtra(EXTRA_WIDGET_NAV)

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
                    
                    LaunchedEffect(pendingWidgetRoute) {
                        pendingWidgetRoute?.let { route ->
                            val navOptions: androidx.navigation.NavOptionsBuilder.() -> Unit = {
                                popUpTo<DashboardRoute> { inclusive = false; saveState = false }
                                launchSingleTop = true
                            }
                            when (route) {
                                "dashboard"      -> navController.navigate(DashboardRoute(), navOptions)
                                "records"        -> navController.navigate(TransactionListRoute(), navOptions)
                                "analytics"      -> navController.navigate(AnalyticsRoute, navOptions)
                                "exchange_rates" -> navController.navigate(ExchangeRatesRoute, navOptions)
                            }
                            pendingWidgetRoute = null
                        }
                    }

                    OssWalletNavGraph(
                        navController = navController,
                        onSplashConditionChanged = { keepSplashOnScreen = it }
                    )
                }
                } // CompositionLocalProvider
            }
        }
    }
}
