package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.AccountDto
import dev.lacruzjs.sobreio.data.remote.dto.AccountStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.AccountsOverviewDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDbValue
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.mapper.toSummary
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.AccountDraft
import dev.lacruzjs.sobreio.domain.model.AccountsSummary
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

/**
 * Row level security decides who may write: only owners and admins. Balances come from the `account_balances` view,
 * which has no colour or icon, so those are read from `accounts` and joined by id (no sums happen in the app).
 */
class SupabaseAccountRepository(client: SupabaseClient) : AccountRepository {

    private val postgrest = client.postgrest

    override suspend fun getAccounts(groupId: String): Result<List<Account>> = safeCall {
        val balances = postgrest.from(BALANCES_VIEW)
            .select(Columns.raw("account_id,group_id,name,kind,in_envelopes,owner_user_id,archived_at,balance")) {
                filter { eq("group_id", groupId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<AccountDto>()
        val styles = postgrest.from(TABLE)
            .select(Columns.list("id", "color", "icon")) { filter { eq("group_id", groupId) } }
            .decodeList<AccountStyleDto>()
            .associateBy { it.id }
        balances.map { it.toDomain(styles[it.id]) }
    }

    override suspend fun getSummary(groupId: String): Result<AccountsSummary> = safeCall {
        postgrest.from(OVERVIEW_VIEW)
            .select(Columns.list("accounts_total", "in_envelopes_total", "outside_total")) {
                filter { eq("group_id", groupId) }
            }
            .decodeSingleOrNull<AccountsOverviewDto>()
            .toSummary()
    }

    override suspend fun createAccount(
        groupId: String,
        draft: AccountDraft,
        inEnvelopes: Boolean,
        openingBalanceCents: Long,
    ): Result<String> = safeCall {
        postgrest.rpc(
            "create_account",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_name", draft.name)
                put("p_kind", draft.kind.toDbValue())
                put("p_opening_balance", openingBalanceCents)
                put("p_in_envelopes", inEnvelopes)
                put("p_owner_user_id", draft.ownerUserId)
                put("p_color", draft.color)
                put("p_icon", draft.icon)
            },
        ).decodeAs<String>()
    }

    override suspend fun updateAccount(id: String, draft: AccountDraft): Result<Unit> = safeCall {
        update(id) {
            set("name", draft.name)
            set("kind", draft.kind.toDbValue())
            set("owner_user_id", draft.ownerUserId)
            set("color", draft.color)
            set("icon", draft.icon)
        }
    }

    override suspend fun changeAccountScope(id: String, inEnvelopes: Boolean): Result<Unit> = safeCall {
        postgrest.rpc(
            "change_account_scope",
            buildJsonObject {
                put("p_account_id", id)
                put("p_in_envelopes", inEnvelopes)
            },
        )
        Unit
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
