package dev.lacruzjs.sobreio.di

import com.russhwolf.settings.Settings
import dev.lacruzjs.sobreio.data.cache.QueryCache
import dev.lacruzjs.sobreio.data.cache.CachedAccountRepository
import dev.lacruzjs.sobreio.data.cache.CachedAccountDetailRepository
import dev.lacruzjs.sobreio.data.cache.CachedBankImportRepository
import dev.lacruzjs.sobreio.data.cache.CachedCommitmentRepository
import dev.lacruzjs.sobreio.data.cache.CachedEnvelopeRepository
import dev.lacruzjs.sobreio.data.cache.CachedEnvelopeDetailRepository
import dev.lacruzjs.sobreio.data.cache.CachedEnvelopeTransferRepository
import dev.lacruzjs.sobreio.data.cache.CachedGroupRepository
import dev.lacruzjs.sobreio.data.cache.CachedGroupSummaryRepository
import dev.lacruzjs.sobreio.data.cache.CachedMemberRepository
import dev.lacruzjs.sobreio.data.cache.CachedMovementRepository
import dev.lacruzjs.sobreio.data.cache.CachedMovementFormRepository
import dev.lacruzjs.sobreio.data.cache.CachedTagRepository
import dev.lacruzjs.sobreio.data.local.LastAccountStore
import dev.lacruzjs.sobreio.data.local.AppPreferencesStore
import dev.lacruzjs.sobreio.data.local.BankFormatStore
import dev.lacruzjs.sobreio.data.local.SettingsBankFormatStore
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.data.local.SettingsLastAccountStore
import dev.lacruzjs.sobreio.data.local.SettingsAppPreferencesStore
import dev.lacruzjs.sobreio.data.local.SettingsLastGroupStore
import dev.lacruzjs.sobreio.data.remote.buildSupabaseClient
import dev.lacruzjs.sobreio.data.repository.AccountDeletionRepository
import dev.lacruzjs.sobreio.data.repository.AccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseAccountDeletionRepository
import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.data.repository.BankImportRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseBankImportRepository
import dev.lacruzjs.sobreio.data.repository.CommitmentRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeDetailRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeTransferRepository
import dev.lacruzjs.sobreio.data.repository.EnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.GroupRepository
import dev.lacruzjs.sobreio.data.repository.GroupSummaryRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseGroupSummaryRepository
import dev.lacruzjs.sobreio.data.repository.MemberRepository
import dev.lacruzjs.sobreio.data.repository.MovementFormRepository
import dev.lacruzjs.sobreio.data.repository.MovementRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseAccountDetailRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseAccountRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseAuthRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseCommitmentRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseEnvelopeDetailRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseEnvelopeTransferRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseEnvelopeRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseGroupRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseMemberRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseMovementFormRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseMovementRepository
import dev.lacruzjs.sobreio.data.repository.SupabaseTagRepository
import dev.lacruzjs.sobreio.data.repository.TagRepository
import org.koin.dsl.module

val dataModule = module {
    // Created on first use, so the app still starts when local.properties has no key yet
    single { QueryCache() }
    single { buildSupabaseClient(get()) }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<GroupRepository> { CachedGroupRepository(SupabaseGroupRepository(get()), get()) }
    single<GroupSummaryRepository> { CachedGroupSummaryRepository(SupabaseGroupSummaryRepository(get()), get()) }
    single<AccountRepository> { CachedAccountRepository(SupabaseAccountRepository(get()), get()) }
    single<AccountDetailRepository> { CachedAccountDetailRepository(SupabaseAccountDetailRepository(get()), get()) }
    single<CommitmentRepository> { CachedCommitmentRepository(SupabaseCommitmentRepository(get()), get()) }
    single<EnvelopeRepository> { CachedEnvelopeRepository(SupabaseEnvelopeRepository(get()), get()) }
    single<EnvelopeDetailRepository> { CachedEnvelopeDetailRepository(SupabaseEnvelopeDetailRepository(get()), get()) }
    single<EnvelopeTransferRepository> { CachedEnvelopeTransferRepository(SupabaseEnvelopeTransferRepository(get()), get()) }
    single<MemberRepository> { CachedMemberRepository(SupabaseMemberRepository(get()), get()) }
    single<MovementRepository> { CachedMovementRepository(SupabaseMovementRepository(get()), get()) }
    single<MovementFormRepository> { CachedMovementFormRepository(SupabaseMovementFormRepository(get()), get()) }
    single<AccountDeletionRepository> { SupabaseAccountDeletionRepository(get(), get(), get()) }
    single<TagRepository> { CachedTagRepository(SupabaseTagRepository(get()), get()) }
    single<BankImportRepository> { CachedBankImportRepository(SupabaseBankImportRepository(get()), get()) }
    single<LastGroupStore> { SettingsLastGroupStore(Settings()) }
    single<LastAccountStore> { SettingsLastAccountStore(Settings()) }
    single<AppPreferencesStore> { SettingsAppPreferencesStore(Settings()) }
    single<BankFormatStore> { SettingsBankFormatStore(Settings()) }
}
