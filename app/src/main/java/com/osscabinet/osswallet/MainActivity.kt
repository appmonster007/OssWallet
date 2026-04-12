package com.osscabinet.ossWallet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.navigation.OssWalletNavGraph
import com.osscabinet.ossWallet.ui.theme.OssWalletTheme

/**
 * Main Activity of the OssWallet application.
 * Responsibility: Entry point and UI theme/navigation hosting.
 * Follows SRP by delegating dependency management to [AppContainer] 
 * and navigation to [OssWalletNavGraph].
 */
class MainActivity : FragmentActivity() {
    
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize dependency container
        appContainer = AppContainer(this)

        setContent {
            OssWalletApp(appContainer)
        }
    }
}

@Composable
fun OssWalletApp(appContainer: AppContainer) {
    val theme by appContainer.settingsDataStore.theme.collectAsState(initial = "System")
    
    OssWalletTheme(theme = theme) {
        val navController = rememberNavController()
        OssWalletNavGraph(
            navController = navController,
            appContainer = appContainer
        )
    }
}
