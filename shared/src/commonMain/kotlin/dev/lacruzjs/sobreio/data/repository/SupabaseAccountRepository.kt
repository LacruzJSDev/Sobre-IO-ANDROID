package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.AccountDto
import dev.lacruzjs.sobreio.data.remote.dto.AccountsTotalDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDbValue
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountKind
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Row level security decides who may write: only owners and admins. Reads come from the `account_balances` view. */
class SupabaseAccountRepository(client: SupabaseClient) : AccountRepository {

    private val postgrest = client.postgrest

    override suspend fun getAccounts(groupId: String): Result<List<Account>> = safeCall {
        postgrest.from(BALANCES_VIEW)
            .select(Columns.raw("account_id,group_id,name,kind,in_envelopes,balance")) {
                filter {
                    eq("group_id", groupId)
                    exact("archived_at", null)
                }
                order("name", Order.ASCENDING)
            }
            .decodeList<AccountDto>()
            .map { it.toDomain() }
    }

    override suspend fun getAccountsTotal(groupId: String): Result<Long> = safeCall {
        postgrest.from(OVERVIEW_VIEW)
            .select(Columns.list("accounts_total")) { filter { eq("group_id", groupId) } }
            .decodeSingleOrNull<AccountsTotalDto>()
            ?.total
            ?: 0L
    }

    override suspend fun createAccount(
        groupId: String,
        name: String,
        kind: AccountKind,
        openingBalanceCents: Long,
    ): Result<String> = safeCall {
        postgrest.rpc(
            "create_account",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_name", name)
                put("p_kind", kind.toDbValue())
                put("p_opening_balance", openingBalanceCents)
            },
        ).decodeAs<String>()
    }

    override suspend fun updateAccount(id: String, name: String, kind: AccountKind): Result<Unit> = safeCall {
        update(id) {
            set("name", name)
            set("kind", kind.toDbValue())
        }
    }

    override suspend fun archiveAccount(id: String): Result<Unit> = safeCall {
        update(id) { set("archived_at", Clock.System.now().toString()) }
    }

    override suspend fun unarchiveAccount(id: String): Result<Unit> = safeCall {
        update(id) { setToNull("archived_at") }
    }

    private suspend fun update(id: String, changes: PostgrestUpdate.() -> Unit) {
        val updated = postgrest.from(TABLE)
            .update(changes) {
                select(Columns.list("id"))
                filter { eq("id", id) }
            }
            .decodeList<IdDto>()
        // Row level security hides the account from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val TABLE = "accounts"
        const val BALANCES_VIEW = "account_balances"
        const val OVERVIEW_VIEW = "group_overview"
    }
}
