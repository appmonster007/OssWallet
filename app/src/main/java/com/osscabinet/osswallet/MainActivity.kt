package com.osscabinet.ossWallet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.navigation.OssWalletNavGraph
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme
import kotlinx.coroutines.launch

/**
 * Main Activity of the OssWallet application.
 * Responsibility: Entry point and UI theme/navigation hosting.
 * Follows SRP by delegating dependency management to [AppContainer] 
 * and navigation to [OssWalletNavGraph].
 */
class MainActivity : FragmentActivity() {
    private var keepSplashOnScreen = true
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize dependency container
        appContainer = (application as OssWalletApp).container

//        // Seed database in debug mode
//        if (BuildConfig.DEBUG) {
//            lifecycleScope.launch {
//                appContainer.databaseSeeder.seed()
//            }
//        }

        setContent {
            OssWalletApp(
                appContainer = appContainer,
                onSplashConditionChanged = { keepSplashOnScreen = it }
            )
        }
    }
}

@Composable
fun OssWalletApp(
    appContainer: AppContainer,
    onSplashConditionChanged: (Boolean) -> Unit = {}
) {
    val theme by appContainer.settingsDataStore.theme.collectAsStateWithLifecycle(initialValue = "System")

    OssWalletTheme(theme = theme) {
        val navController = rememberNavController()
        OssWalletNavGraph(
            navController = navController,
            appContainer = appContainer,
            onSplashConditionChanged = onSplashConditionChanged
        )
    }
}
