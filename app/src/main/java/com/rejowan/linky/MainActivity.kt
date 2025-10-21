package com.rejowan.linky

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.rejowan.linky.presentation.navigation.LinkyNavHost
import com.rejowan.linky.ui.theme.LinkyTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        // Handle incoming share intent
        handleIntent(intent)

        setContent {
            LinkyTheme {
                val navController = rememberNavController()

                LinkyNavHost(
                    navController = navController,
                    isAuthRequired = false,
                    sharedUrl = sharedUrl,
                    onSharedUrlHandled = { sharedUrl = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!text.isNullOrBlank()) {
                        Timber.d("Received shared text: $text")
                        // Extract URL from text (handles cases like "Check this: https://example.com cool!")
                        val extractedUrl = extractUrlFromText(text)
                        if (extractedUrl != null) {
                            Timber.d("Extracted URL: $extractedUrl")
                            sharedUrl = extractedUrl
                        } else {
                            Timber.d("No URL found in shared text")
                        }
                    }
                }
            }
        }
    }

    /**
     * Extract URL from text that may contain additional content
     * Handles cases like: "john doe https://johndoe.com djf"
     * Returns the first valid URL found, or null if none found
     */
    private fun extractUrlFromText(text: String): String? {
        // Regex pattern to match URLs with or without protocol
        val urlPattern = Regex(
            """(?:https?://)?(?:www\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*)""",
            RegexOption.IGNORE_CASE
        )

        val match = urlPattern.find(text.lowercase())
        var url = match?.value

        // If URL doesn't start with protocol, add https://
        if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        return url?.lowercase()
    }
}
