package dev.mathieuburnat.piratefocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.mathieuburnat.piratefocus.ui.FocusRoute
import dev.mathieuburnat.piratefocus.ui.theme.PirateFocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PirateFocusTheme {
                FocusRoute()
            }
        }
    }
}
