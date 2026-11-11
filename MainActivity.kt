package com.autophoto.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.autophoto.ai.ui.navigation.AppNavHost
import com.autophoto.ai.ui.theme.AutoPhotoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoPhotoTheme { AppNavHost() }
        }
    }
}
