package dev.mathieuburnat.piratefocus.guard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.MainActivity
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.focus.PirateQuotes
import dev.mathieuburnat.piratefocus.ui.PixelPirate
import dev.mathieuburnat.piratefocus.ui.TypewriterText
import dev.mathieuburnat.piratefocus.ui.theme.PirateFocusTheme

/** L'écran qui surgit quand on ouvre une appli interdite en pleine traversée. */
class CaughtActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appName = intent.getStringExtra(EXTRA_PACKAGE)?.let(::labelFor) ?: "cette appli"

        setContent {
            PirateFocusTheme {
                BackHandler { backToShip() }
                var quote by remember { mutableStateOf(PirateQuotes.caught(appName)) }
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.safeDrawingPadding().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("!!! À L'ABORDAGE !!!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        PixelPirate(phase = Phase.SUNK)
                        Spacer(Modifier.height(16.dp))
                        TypewriterText(
                            "« $quote »",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable { quote = PirateQuotes.caught(appName, current = quote) },
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = ::backToShip, shape = RectangleShape, modifier = Modifier.fillMaxWidth()) {
                            Text("> RETOUR AU NAVIRE")
                        }
                    }
                }
            }
        }
    }

    private fun backToShip() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        finish()
    }

    private fun labelFor(packageName: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    companion object {
        private const val EXTRA_PACKAGE = "package"

        fun intent(context: Context, packageName: String): Intent =
            Intent(context, CaughtActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
