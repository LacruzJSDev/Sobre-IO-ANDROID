package dev.lacruzjs.sobreio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.lacruzjs.sobreio.navigation.NavigationWrapper
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SobreioTheme {
                NavigationWrapper()
            }
        }
    }
}
