package com.rejowan.linky.di

import com.rejowan.linky.data.export.ExportManager
import com.rejowan.linky.data.export.ImportManager
import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.LinkPreviewFetcher
import com.rejowan.linky.util.PreferencesManager
import com.rejowan.linky.util.ReaderModeParser
import org.koin.dsl.module

val appModule = module {
    single { ThemePreferences(get()) }
    single { PreferencesManager(get()) }
    single { LinkPreviewFetcher() }
    single { FileStorageManager(get()) }
    single { ReaderModeParser() }

    // Export/Import managers
    single { ExportManager(get(), get(), get(), get()) }
    single { ImportManager(get(), get(), get(), get()) }
}
