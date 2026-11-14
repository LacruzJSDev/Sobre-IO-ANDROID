package dev.lacruzjs.sobreio.di

import com.russhwolf.settings.Settings
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.data.local.SettingsLastGroupStore
import dev.lacruzjs.sobreio.data.remote.buildSupabaseClient
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseAccountRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseAuthRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseEnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseGroupRepository
import org.koin.dsl.module

val dataModule = module {
    // Created on first use, so the app still starts when local.properties has no key yet
    single { buildSupabaseClient() }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<GroupRepository> { SupabaseGroupRepository(get()) }
    single<AccountRepository> { SupabaseAccountRepository(get()) }
    single<EnvelopeRepository> { SupabaseEnvelopeRepository(get()) }
    single<LastGroupStore> { SettingsLastGroupStore(Settings()) }
}
