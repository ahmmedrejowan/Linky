package com.rejowan.linky

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.di.appModule
import com.rejowan.linky.di.databaseModule
import com.rejowan.linky.di.dataStoreModule
import com.rejowan.linky.di.networkModule
import com.rejowan.linky.di.repositoryModule
import com.rejowan.linky.di.useCaseModule
import com.rejowan.linky.di.vaultModule
import com.rejowan.linky.di.viewModelModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.rejowan.linky.util.DataPreloader
import org.koin.android.ext.android.inject
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
                    appModule,
                    databaseModule,
                    dataStoreModule,
                    networkModule,
                    repositoryModule,
                    useCaseModule,
                    vaultModule,
                    viewModelModule
                )
            )
        }

        // Preload data to warm up cache for faster screen loads
        val dataPreloader: DataPreloader by inject()
        dataPreloader.preload()
    }
}
