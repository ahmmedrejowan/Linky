package com.rejowan.linky.di

import com.rejowan.linky.data.repository.VaultRepositoryImpl
import com.rejowan.linky.data.security.VaultCryptoManager
import com.rejowan.linky.data.security.VaultSessionManager
import com.rejowan.linky.domain.repository.VaultRepository
import com.rejowan.linky.domain.usecase.vault.AddVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.ChangeVaultPinUseCase
import com.rejowan.linky.domain.usecase.vault.ClearVaultUseCase
import com.rejowan.linky.domain.usecase.vault.DeleteVaultLinkUseCase
import com.rejowan.linky.domain.usecase.vault.GetAllVaultLinksUseCase
import com.rejowan.linky.domain.usecase.vault.GetVaultLinkByIdUseCase
import com.rejowan.linky.domain.usecase.vault.LockVaultUseCase
import com.rejowan.linky.domain.usecase.vault.SetupVaultPinUseCase
import com.rejowan.linky.domain.usecase.vault.UnlockVaultUseCase
import com.rejowan.linky.domain.usecase.vault.UpdateVaultLinkUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val vaultModule = module {
    // Security
    single { VaultCryptoManager(androidContext()) }
    single { VaultSessionManager(get()) }

    // Repository
    single<VaultRepository> {
        VaultRepositoryImpl(
            context = androidContext(),
            vaultLinkDao = get(),
            pendingVaultLinkDao = get(),
            cryptoManager = get(),
            sessionManager = get()
        )
    }

    // Use Cases
    factory { SetupVaultPinUseCase(get()) }
    factory { UnlockVaultUseCase(get()) }
    factory { LockVaultUseCase(get()) }
    factory { GetAllVaultLinksUseCase(get()) }
    factory { GetVaultLinkByIdUseCase(get()) }
    factory { AddVaultLinkUseCase(get()) }
    factory { UpdateVaultLinkUseCase(get()) }
    factory { DeleteVaultLinkUseCase(get()) }
    factory { ChangeVaultPinUseCase(get()) }
    factory { ClearVaultUseCase(get()) }
}
