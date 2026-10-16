package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BatchProcessingScreen
import com.example.ui.screens.ComparisonStudioScreen
import com.example.ui.screens.ExportStudioScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.screens.PrecisionEditorScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoFrameApp()
                }
            }
        }
    }
}

@Composable
fun PhotoFrameApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle Android system back gesture
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        viewModel.navigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        when (currentScreen) {
            AppScreen.HOME -> HomeScreen(viewModel)
            AppScreen.VIDEO_DETAILS -> VideoDetailScreen(viewModel)
            AppScreen.PRECISION_EDITOR -> PrecisionEditorScreen(viewModel)
            AppScreen.PROCESSING -> ProcessingScreen(viewModel)
            AppScreen.GALLERY -> GalleryScreen(viewModel)
            AppScreen.PHOTO_VIEWER -> PhotoViewerScreen(viewModel)
            AppScreen.COMPARISON_STUDIO -> ComparisonStudioScreen(viewModel)
            AppScreen.EXPORT_STUDIO -> ExportStudioScreen(viewModel)
            AppScreen.BATCH_PROCESSING -> BatchProcessingScreen(viewModel)
            AppScreen.SETTINGS -> SettingsScreen(viewModel)
            AppScreen.HELP -> HelpScreen(viewModel)
            AppScreen.ABOUT -> SettingsScreen(viewModel)
        }
    }
}
