package com.websitecheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.websitecheck.app.ui.WebsiteCheckApp
import com.websitecheck.app.ui.theme.WebsiteCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer = (application as com.websitecheck.app.WebsiteCheckApp).container
        setContent {
            val settings by appContainer.settingsRepository.settings.collectAsStateWithLifecycle()
            WebsiteCheckTheme(themeMode = settings.themeMode) {
                WebsiteCheckApp()
            }
        }
    }
}
