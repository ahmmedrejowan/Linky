package com.rejowan.linky

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.rejowan.linky.presentation.navigation.LinkyNavHost
import com.rejowan.linky.presentation.navigation.SharedContent
import com.rejowan.linky.ui.theme.LinkyTheme
import com.rejowan.linky.util.PreferencesManager
import com.rejowan.linky.util.UrlExtractor
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private var sharedContent by mutableStateOf<SharedContent?>(null)
    private lateinit var preferencesManager: PreferencesManager

    // Splash screen state
    private var isReady by mutableStateOf(false)
    private var showOnboarding by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash screen visible until we're ready
        splashScreen.setKeepOnScreenCondition { !isReady }

        preferencesManager = PreferencesManager(this)

        // Load preferences in background
        lifecycleScope.launch {
            showOnboarding = !preferencesManager.hasCompletedOnboarding()
            isReady = true
        }

        // Handle incoming share intent
        handleIntent(intent)

        setContent {
            LinkyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isReady) {
                        val navController = rememberNavController()
                        var onboardingVisible by remember { mutableStateOf(showOnboarding) }

                        LinkyNavHost(
                            navController = navController,
                            showOnboarding = onboardingVisible,
                            onOnboardingComplete = {
                                preferencesManager.setOnboardingCompleted()
                                onboardingVisible = false
                            },
                            sharedContent = sharedContent,
                            onSharedContentHandled = { sharedContent = null }
                        )
                    }
                }
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
                    // Get title/subject if available (many browsers share the page title here)
                    val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

                    if (!text.isNullOrBlank()) {
                        Timber.d("Received shared text: $text")
                        if (!title.isNullOrBlank()) {
                            Timber.d("Received shared title: $title")
                        }

                        // Extract all URLs from the shared text
                        val urls = UrlExtractor.extractUrls(text)
                        Timber.d("Extracted ${urls.size} URL(s)")

                        if (urls.isNotEmpty()) {
                            sharedContent = SharedContent(
                                text = text,
                                urls = urls,
                                title = title
                            )
                            Timber.d("Smart intent handling: ${urls.size} URLs detected")
                        } else {
                            Timber.d("No URLs found in shared text")
                        }
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                // Handle direct URL viewing (e.g., user selects "Open with Linky" for a link)
                val url = intent.data?.toString()
                if (!url.isNullOrBlank()) {
                    Timber.d("Received VIEW intent for URL: $url")
                    sharedContent = SharedContent.fromUrl(url)
                }
            }
        }
    }
}
