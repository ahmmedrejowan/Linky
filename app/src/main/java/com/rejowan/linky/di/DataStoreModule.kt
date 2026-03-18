package com.rejowan.linky.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

// DataStore instance for update settings
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

val dataStoreModule = module {
    // Settings DataStore (used by UpdateRepository for last check time, skipped versions)
    single { androidContext().settingsDataStore }
}
