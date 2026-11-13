package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.ai.configureAppCheck
import com.example.ui.CSPluginStudioApp
import com.example.ui.theme.CSPluginStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before rendering UI or creating ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            CSPluginStudioTheme {
                CSPluginStudioApp()
            }
        }
    }
}
