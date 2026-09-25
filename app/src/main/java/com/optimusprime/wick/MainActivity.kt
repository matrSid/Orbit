package com.optimusprime.wick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.optimusprime.wick.ui.nav.WickNavGraph
import com.optimusprime.wick.ui.theme.WickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WickTheme {
                WickNavGraph()
            }
        }
    }
}
