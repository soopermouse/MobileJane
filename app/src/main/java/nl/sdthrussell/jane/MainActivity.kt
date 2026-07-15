package nl.sdthrussell.jane

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import nl.sdthrussell.jane.ui.JaneApp
import nl.sdthrussell.jane.ui.theme.JaneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JaneTheme {
                JaneApp()
            }
        }
    }
}
