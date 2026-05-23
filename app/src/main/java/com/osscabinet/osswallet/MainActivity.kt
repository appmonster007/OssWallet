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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.osscabinet.ossWallet.ui.navigation.Screen
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import android.content.Intent
import com.osscabinet.ossWallet.ui.theme.AppColors

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
            val precision by settingsRepository.decimalPrecision.collectAsStateWithLifecycle(initialValue = 2)
            val use24Hour by settingsRepository.use24HourFormat.collectAsStateWithLifecycle(initialValue = false)

            LaunchedEffect(precision) {
                com.osscabinet.ossWallet.domain.service.currency.CurrencyFormatter.precision = precision
                com.osscabinet.ossWallet.domain.service.currency.CurrencyService.precision = precision
                com.osscabinet.ossWallet.domain.service.timeline.TimelineService.precision = precision
            }

            LaunchedEffect(use24Hour) {
                com.osscabinet.ossWallet.ui.util.TimeUtils.use24HourFormat = use24Hour
            }

            OssWalletTheme(theme = theme, fontName = selectedFont) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.screenBackground
                ) {
                    val navController = rememberNavController()
                    
                    LaunchedEffect(pendingWidgetRoute) {
                        pendingWidgetRoute?.let { route ->
                            when (route) {
                                "dashboard" -> navController.navigate(Screen.Main.createRoute("dashboard"))
                                "records" -> navController.navigate(Screen.Main.createRoute("records"))
                                "analytics" -> navController.navigate(Screen.Main.createRoute("analytics"))
                                "exchange_rates" -> navController.navigate(Screen.Main.createRoute("exchange_rates"))
                            }
                            pendingWidgetRoute = null
                        }
                    }

                    OssWalletNavGraph(
                        navController = navController,
                        onSplashConditionChanged = { keepSplashOnScreen = it }
                    )
                }
            }
        }
    }
}
