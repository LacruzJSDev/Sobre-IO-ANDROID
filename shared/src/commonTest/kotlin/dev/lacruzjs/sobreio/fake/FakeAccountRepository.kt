package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountDraft
import dev.lacruzjs.sobreio.domain.model.AccountsSummary
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import kotlinx.datetime.LocalDate

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeAccountRepository(var accounts: List<Account> = emptyList()) : AccountRepository {

    val calls = mutableListOf<String>()

    /** What the last create and update received, with every field. */
    var createdDraft: AccountDraft? = null
    var createdInEnvelopes: Boolean? = null
    var createdOpeningBalance: Long? = null
    var updatedDraft: AccountDraft? = null

    var accountsResult: Result<List<Account>>? = null
    var summary = AccountsSummary(totalCents = 0, inEnvelopesCents = 0, outsideCents = 0)
    var summaryResult: Result<AccountsSummary>? = null
    var createResult: Result<String> = Result.success("new-id")
    var updateResult: Result<Unit> = Result.success(Unit)
    var scopeResult: Result<Unit> = Result.success(Unit)
    var archiveResult: Result<Unit> = Result.success(Unit)
    var unarchiveResult: Result<Unit> = Result.success(Unit)

    override suspend fun getAccounts(groupId: String): Result<List<Account>> {
        calls += "getAccounts($groupId)"
        return accountsResult ?: Result.success(accounts)
    }

    override suspend fun getSummary(groupId: String): Result<AccountsSummary> {
        calls += "getSummary($groupId)"
        return summaryResult ?: Result.success(summary)
    }

    var balanceSeries = emptyList<BalancePoint>()
    var balanceSeriesResult: Result<List<BalancePoint>>? = null

    /** What the last series read asked for. */
    var seriesRange: Pair<LocalDate, LocalDate>? = null
    var seriesPoints: Int? = null

    override suspend fun getBalanceSeries(groupId: String, from: LocalDate, to: LocalDate, points: Int): Result<List<BalancePoint>> {
        calls += "getBalanceSeries($groupId, $from, $to)"
        seriesRange = from to to
        seriesPoints = points
        return balanceSeriesResult ?: Result.success(balanceSeries)
    }

    override suspend fun createAccount(
        groupId: String,
        draft: AccountDraft,
        inEnvelopes: Boolean,
        openingBalanceCents: Long,
    ): Result<String> {
        calls += "createAccount($groupId, ${draft.name}, ${draft.kind}, $openingBalanceCents)"
        createdDraft = draft
        createdInEnvelopes = inEnvelopes
        createdOpeningBalance = openingBalanceCents
        return createResult
    }

    override suspend fun updateAccount(id: String, draft: AccountDraft): Result<Unit> {
        calls += "updateAccount($id, ${draft.name}, ${draft.kind})"
        updatedDraft = draft
        return updateResult
    }

    override suspend fun changeAccountScope(id: String, inEnvelopes: Boolean): Result<Unit> {
        calls += "changeAccountScope($id, $inEnvelopes)"
        return scopeResult
    }

    override suspend fun archiveAccount(id: String): Result<Unit> {
        calls += "archiveAccount($id)"
        return archiveResult
    }

    override suspend fun unarchiveAccount(id: String): Result<Unit> {
        calls += "unarchiveAccount($id)"
        return unarchiveResult
    }
}
