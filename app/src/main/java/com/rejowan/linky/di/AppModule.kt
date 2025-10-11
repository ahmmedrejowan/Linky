package com.rejowan.linky.di

import com.rejowan.linky.data.local.preferences.ThemePreferences
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ThemePreferences(get()) }
}
