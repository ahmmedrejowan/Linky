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
import com.rejowan.linky.presentation.navigation.SharedContent
import com.rejowan.linky.ui.theme.LinkyTheme
import com.rejowan.linky.util.UrlExtractor
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private var sharedContent by mutableStateOf<SharedContent?>(null)

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
                    sharedContent = sharedContent,
                    onSharedContentHandled = { sharedContent = null }
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

                        // Extract all URLs from the shared text
                        val urls = UrlExtractor.extractUrls(text)
                        Timber.d("Extracted ${urls.size} URL(s)")

                        if (urls.isNotEmpty()) {
                            sharedContent = SharedContent(
                                text = text,
                                urls = urls
                            )
                            Timber.d("Smart intent handling: ${urls.size} URLs detected")
                        } else {
                            Timber.d("No URLs found in shared text")
                        }
                    }
                }
            }
        }
    }
}
