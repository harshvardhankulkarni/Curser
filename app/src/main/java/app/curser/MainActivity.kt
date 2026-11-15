package app.curser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.curser.ui.CurserApp
import app.curser.ui.CurserViewModel
import app.curser.ui.theme.CurserTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CurserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CurserTheme {
                CurserApp(viewModel.controller)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.controller.start()
    }

    override fun onStop() {
        viewModel.controller.stop()
        super.onStop()
    }
}
