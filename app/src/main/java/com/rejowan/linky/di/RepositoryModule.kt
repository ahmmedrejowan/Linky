package com.rejowan.linky.di

import com.rejowan.linky.data.repository.CollectionRepositoryImpl
import com.rejowan.linky.data.repository.LinkRepositoryImpl
import com.rejowan.linky.data.repository.SnapshotRepositoryImpl
import com.rejowan.linky.data.repository.TagRepositoryImpl
import com.rejowan.linky.domain.repository.CollectionRepository
import com.rejowan.linky.domain.repository.LinkRepository
import com.rejowan.linky.domain.repository.SnapshotRepository
import com.rejowan.linky.domain.repository.TagRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<LinkRepository> { LinkRepositoryImpl(get()) }
    single<CollectionRepository> { CollectionRepositoryImpl(get()) }
    single<SnapshotRepository> { SnapshotRepositoryImpl(get()) }
    single<TagRepository> { TagRepositoryImpl(get()) }
}
