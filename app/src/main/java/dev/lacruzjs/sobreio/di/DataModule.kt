package dev.lacruzjs.sobreio.di

import dev.lacruzjs.sobreio.data.remote.buildSupabaseClient
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseAuthRepository
import org.koin.dsl.module

val dataModule = module {
    // Created on first use, so the app still starts when local.properties has no key yet
    single { buildSupabaseClient() }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
}
