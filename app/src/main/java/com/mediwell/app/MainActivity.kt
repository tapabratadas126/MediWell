package com.mediwell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mediwell.app.ui.navigation.AppNavHost
import com.mediwell.app.ui.theme.MediwellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediwellTheme {
                AppNavHost()
            }
        }
    }
}
