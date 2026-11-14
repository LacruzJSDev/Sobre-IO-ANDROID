package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopesOverviewDto
import dev.lacruzjs.sobreio.data.remote.mapper.toDbValue
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.mapper.toSummary
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeDraft
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
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
 * Row level security decides who may write: owners and admins manage envelopes, any member assigns money. Reads come
 * from the `envelope_balances` and `group_overview` views.
 */
class SupabaseEnvelopeRepository(client: SupabaseClient) : EnvelopeRepository {

    private val postgrest = client.postgrest

    override suspend fun materializeThroughToday(groupId: String): Result<Unit> = safeCall {
        postgrest.rpc("materialize_through_today", buildJsonObject { put("p_group_id", groupId) })
        Unit
    }

    override suspend fun getEnvelopes(groupId: String): Result<List<Envelope>> = safeCall {
        val balances = postgrest.from(BALANCES_VIEW)
            .select(Columns.raw("envelope_id,group_id,name,kind,owner_user_id,archived_at,monthly_target,balance")) {
                filter { eq("group_id", groupId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<EnvelopeDto>()
        // The balances view has no colour or icon: they come from `envelopes`, joined by id
        val styles = postgrest.from(TABLE)
            .select(Columns.list("id", "color", "icon")) { filter { eq("group_id", groupId) } }
            .decodeList<EnvelopeStyleDto>()
            .associateBy { it.id }
        balances.mapNotNull { it.toDomain(styles[it.id]) }
    }

    override suspend fun getSummary(groupId: String): Result<EnvelopeSummary> = safeCall {
        postgrest.from(OVERVIEW_VIEW)
            .select(Columns.list("in_envelopes_total", "unassigned_balance")) { filter { eq("group_id", groupId) } }
            .decodeSingleOrNull<EnvelopesOverviewDto>()
            .toSummary()
    }

    override suspend fun createEnvelope(groupId: String, draft: EnvelopeDraft): Result<String> = safeCall {
        postgrest.from(TABLE)
            .insert(
                buildJsonObject {
                    put("group_id", groupId)
                    put("name", draft.name)
                    put("kind", draft.kind.toDbValue())
                    put("monthly_target", draft.monthlyTargetCents)
                    put("owner_user_id", draft.ownerUserId)
                    put("color", draft.color)
                    put("icon", draft.icon)
                },
            ) { select(Columns.list("id")) }
            .decodeSingle<IdDto>()
            .id
    }

    override suspend fun updateEnvelope(id: String, draft: EnvelopeDraft): Result<Unit> = safeCall {
        update(id) {
            set("name", draft.name)
            set("kind", draft.kind.toDbValue())
            set("monthly_target", draft.monthlyTargetCents)
            set("owner_user_id", draft.ownerUserId)
            set("color", draft.color)
            set("icon", draft.icon)
        }
    }

    override suspend fun archiveEnvelope(id: String): Result<Unit> = safeCall {
        update(id) { set("archived_at", Clock.System.now().toString()) }
    }

    override suspend fun unarchiveEnvelope(id: String): Result<Unit> = safeCall {
        update(id) { setToNull("archived_at") }
    }

    override suspend fun assign(
        groupId: String,
        fromEnvelopeId: String,
        toEnvelopeId: String,
        amountCents: Long,
    ): Result<Unit> = safeCall {
        // period_id is set by a trigger and origin defaults to 'manual'
        postgrest.from(TRANSFERS_TABLE).insert(
            buildJsonObject {
                put("group_id", groupId)
                put("from_envelope_id", fromEnvelopeId)
                put("to_envelope_id", toEnvelopeId)
                put("amount", amountCents)
            },
        )
        Unit
    }

    private suspend fun update(id: String, changes: PostgrestUpdate.() -> Unit) {
        val updated = postgrest.from(TABLE)
            .update(changes) {
                select(Columns.list("id"))
                filter { eq("id", id) }
            }
            .decodeList<IdDto>()
        // Row level security hides the envelope from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val TABLE = "envelopes"
        const val TRANSFERS_TABLE = "envelope_transfers"
        const val BALANCES_VIEW = "envelope_balances"
        const val OVERVIEW_VIEW = "group_overview"
    }
}
