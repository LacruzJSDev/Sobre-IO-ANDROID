package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind

/** The accounts of a group. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface AccountRepository {

    /** Live (not archived) accounts of the group, ordered by name, with the balance the database computed. */
    suspend fun getAccounts(groupId: String): Result<List<Account>>

    /** Total of the group's accounts in cents, as the database computes it. */
    suspend fun getAccountsTotal(groupId: String): Result<Long>

    /**
     * Creates an account and records [openingBalanceCents] (negative for debt, or 0) as an adjustment, atomically;
     * returns its id. Fails with [dev.lacruzjs.sobreio.domain.error.AppError.DuplicateName] when the name is taken.
     */
    suspend fun createAccount(
        groupId: String,
        name: String,
        kind: AccountKind,
        openingBalanceCents: Long,
    ): Result<String>

    /** Owner or admin only; anyone else fails with a business rule error (FZ016). */
    suspend fun updateAccount(id: String, name: String, kind: AccountKind): Result<Unit>

    /** Archives the account. Fails with FZ008 unless the balance is zero and it has no live commitments. */
    suspend fun archiveAccount(id: String): Result<Unit>

    /** Brings an archived account back. */
    suspend fun unarchiveAccount(id: String): Result<Unit>
}
