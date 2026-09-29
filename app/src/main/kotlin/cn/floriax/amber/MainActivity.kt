package cn.floriax.amber

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import cn.floriax.amber.shared.designsystem.theme.AmberTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Root entry point [ComponentActivity] of the application, hosting the Compose root layout and applying the [AmberTheme].
 *
 * Enables Edge-to-Edge display mode and sets Compose content wrapped in [AmberTheme] within a [Scaffold]
 * container, with innerPadding available to handle system bars insets.
 *
 * @author WangZhiYao
 * @since 2026/9/29
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmberTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                }
            }
        }
    }
}
