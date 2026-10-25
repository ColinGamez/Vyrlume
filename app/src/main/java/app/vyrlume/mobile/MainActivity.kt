package app.vyrlume.mobile

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
        enableEdgeToEdge()
        setContent {
            VyrlumeTheme {
                VyrlumeApp(viewModel)
            }
        }
    }
}
