package com.osscabinet.gleemanager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.osscabinet.gleemanager.di.AppContainer
import com.osscabinet.gleemanager.ui.navigation.GleeNavGraph
import com.osscabinet.gleemanager.ui.theme.GleeManagerTheme

/**
 * Main Activity of the GleeManager application.
 * Responsibility: Entry point and UI theme/navigation hosting.
 * Follows SRP by delegating dependency management to [AppContainer] 
 * and navigation to [GleeNavGraph].
 */
class MainActivity : FragmentActivity() {
    
    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize dependency container
        appContainer = AppContainer(this)

        setContent {
            GleeApp(appContainer)
        }
    }
}

@Composable
fun GleeApp(appContainer: AppContainer) {
    val theme by appContainer.settingsDataStore.theme.collectAsState(initial = "System")
    
    GleeManagerTheme(theme = theme) {
        val navController = rememberNavController()
        GleeNavGraph(
            navController = navController,
            appContainer = appContainer
        )
    }
}
