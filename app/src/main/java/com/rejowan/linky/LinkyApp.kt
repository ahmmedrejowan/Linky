package com.rejowan.linky

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class LinkyApp : Application() {


    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate() {
        super.onCreate()

        themePreferences = ThemePreferences(this)

        coroutineScope.launch {
            themePreferences.setDefaultThemeIfNotSet()
        }

        coroutineScope.launch {
            themePreferences.getTheme().collectLatest { theme ->
                Timber.tag("MAUApp").e("Theme: $theme")
                when (theme) {
                    "Light" -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        Timber.tag("MAUApp").e("Set mode: ${AppCompatDelegate.getDefaultNightMode()}")
                    }

                    "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@LinkyApp)
            modules(
                listOf(
                    appModule
                )
            )
        }

    }
}
