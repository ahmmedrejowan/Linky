package com.rejowan.linky.di

import com.rejowan.linky.data.local.preferences.ThemePreferences
import com.rejowan.linky.util.FileStorageManager
import com.rejowan.linky.util.LinkPreviewFetcher
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ThemePreferences(get()) }
    single { LinkPreviewFetcher() }
    single { FileStorageManager(get()) }
}
