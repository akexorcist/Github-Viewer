package dev.akexorcist.githubviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.akexorcist.githubviewer.ui.navigation.GithubViewerNavHost
import dev.akexorcist.githubviewer.ui.theme.GithubViewerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GithubViewerTheme {
                GithubViewerNavHost()
            }
        }
    }
}
