package com.bacos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bacos.app.ui.AppRoot
import com.bacos.app.ui.theme.BacosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BacosTheme(darkTheme = true) {
                AppRoot()
            }
        }
    }
}
