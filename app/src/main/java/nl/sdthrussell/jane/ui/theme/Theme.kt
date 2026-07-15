package nl.sdthrussell.jane.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JaneColors = darkColorScheme(
    primary = Color(0xFFB7C9FF),
    onPrimary = Color(0xFF17203A),
    secondary = Color(0xFFB9D7CB),
    tertiary = Color(0xFFE7B9D2),
    background = Color(0xFF101319),
    surface = Color(0xFF171B22),
    surfaceVariant = Color(0xFF222833)
)

@Composable
fun JaneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JaneColors,
        content = content
    )
}
