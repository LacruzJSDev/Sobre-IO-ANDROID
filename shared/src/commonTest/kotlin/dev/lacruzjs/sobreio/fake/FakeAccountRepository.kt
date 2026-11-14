package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.AccountRepository
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind

/** Calls are recorded in [calls]; set the `*Result` fields to script failures. */
class FakeAccountRepository(var accounts: List<Account> = emptyList()) : AccountRepository {

    val calls = mutableListOf<String>()

    var accountsResult: Result<List<Account>>? = null
    var total: Long = 0
    var totalResult: Result<Long>? = null
    var createResult: Result<String> = Result.success("new-id")
    var updateResult: Result<Unit> = Result.success(Unit)
    var archiveResult: Result<Unit> = Result.success(Unit)
    var unarchiveResult: Result<Unit> = Result.success(Unit)

    override suspend fun getAccounts(groupId: String): Result<List<Account>> {
        calls += "getAccounts($groupId)"
        return accountsResult ?: Result.success(accounts)
    }

    override suspend fun getAccountsTotal(groupId: String): Result<Long> {
        calls += "getAccountsTotal($groupId)"
        return totalResult ?: Result.success(total)
    }

    override suspend fun createAccount(
        groupId: String,
        name: String,
        kind: AccountKind,
        openingBalanceCents: Long,
    ): Result<String> {
        calls += "createAccount($groupId, $name, $kind, $openingBalanceCents)"
        return createResult
    }

    override suspend fun updateAccount(id: String, name: String, kind: AccountKind): Result<Unit> {
        calls += "updateAccount($id, $name, $kind)"
        return updateResult
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
