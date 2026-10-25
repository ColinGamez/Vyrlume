package app.vyrlume.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.vyrlume.mobile.ui.VyrlumeApp
import app.vyrlume.mobile.ui.theme.VyrlumeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VyrlumeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleConnectionIntent(intent)
        enableEdgeToEdge()
        setContent {
            VyrlumeTheme {
                VyrlumeApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleConnectionIntent(intent)
    }

    private fun handleConnectionIntent(intent: Intent?) {
        val data = intent?.data
        if (intent?.action != Intent.ACTION_VIEW || data?.scheme != "vyrlume" || data.host != "connect") return
        val url = data.getQueryParameter("url") ?: return
        val name = data.getQueryParameter("name")?.ifBlank { null } ?: "USB VS Code"
        viewModel.connect(name, url)
    }
}
