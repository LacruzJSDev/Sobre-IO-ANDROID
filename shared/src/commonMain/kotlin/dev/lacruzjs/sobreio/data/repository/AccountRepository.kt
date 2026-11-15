package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountDraft
import dev.lacruzjs.sobreio.domain.model.AccountsSummary
import dev.lacruzjs.sobreio.domain.model.BalancePoint
import kotlinx.datetime.LocalDate

/** The accounts of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface AccountRepository {

    /**
     * Every account of the group, live and archived (see [Account.isArchived]), ordered by name, with the balance the
     * database computed and the colour and icon read from the `accounts` table.
     */
    suspend fun getAccounts(groupId: String): Result<List<Account>>

    /** Total, money inside the envelopes and money outside them, in cents, as the database computes them. */
    suspend fun getSummary(groupId: String): Result<AccountsSummary>

    /**
     * `group_balance_series`: the total balance of all the live accounts of the group at up to [points] evenly spaced
     * days from [from] to [to] (the last one is [to]), oldest first. The database sums; nothing is summed here.
     */
    suspend fun getBalanceSeries(groupId: String, from: LocalDate, to: LocalDate, points: Int = 60): Result<List<BalancePoint>>

    /**
     * Creates an account with every field and records [openingBalanceCents] (negative for debt, or 0) as an adjustment,
     * atomically; returns its id. An account born outside the envelopes ([inEnvelopes] false) keeps its opening balance
     * outside them. Fails with [dev.lacruzjs.sobreio.domain.error.AppError.DuplicateName] when the name is taken.
     */
    suspend fun createAccount(
        groupId: String,
        draft: AccountDraft,
        inEnvelopes: Boolean,
        openingBalanceCents: Long,
    ): Result<String>

    /** Updates name, kind, holder, colour and icon. Owner or admin only; anyone else fails with FZ016. */
    suspend fun updateAccount(id: String, draft: AccountDraft): Result<Unit>

    /**
     * Moves the account into or out of the envelopes from today (`change_account_scope`). Fails with FZ017 when the
     * account already has movements dated today or later.
     */
    suspend fun changeAccountScope(id: String, inEnvelopes: Boolean): Result<Unit>

    /** Archives the account. Fails with FZ008 unless the balance is zero and it has no live commitments. */
    suspend fun archiveAccount(id: String): Result<Unit>

    /** Brings an archived account back. Fails with a duplicate name when a live account took its name. */
    suspend fun unarchiveAccount(id: String): Result<Unit>
}
