package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.DueOccurrenceDto
import dev.lacruzjs.sobreio.data.remote.dto.ForecastDto
import dev.lacruzjs.sobreio.data.remote.dto.GroupOutlookDto
import dev.lacruzjs.sobreio.data.remote.mapper.toCoverJson
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.mapper.toShortfalls
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AccountShortfall
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.ConfirmedFact
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.DueOccurrence
import dev.lacruzjs.sobreio.domain.model.GroupOutlook
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/** Reads the Group tab through the database's read functions and views; writes through `confirm_occurrence` and `skip_occurrence`. */
class SupabaseGroupSummaryRepository(client: SupabaseClient) : GroupSummaryRepository {

    private val postgrest = client.postgrest

    override suspend fun getOutlook(groupId: String): Result<GroupOutlook> = safeCall {
        postgrest.from("group_outlook_current")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<GroupOutlookDto>()
            .firstOrNull()
            ?.toDomain()
            ?: throw AppError.NotFound
    }

    override suspend fun getPending(groupId: String): Result<List<DueOccurrence>> = safeCall {
        postgrest.rpc("pending_occurrences", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<DueOccurrenceDto>()
            .map { it.toDomain() }
    }

    override suspend fun getUpcoming(groupId: String, from: LocalDate, to: LocalDate): Result<List<DueOccurrence>> = safeCall {
        postgrest.rpc(
            "upcoming_occurrences",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_from", from.toString())
                put("p_to", to.toString())
            },
        ).decodeList<DueOccurrenceDto>().map { it.toDomain() }
    }

    override suspend fun getShortfalls(groupId: String): Result<List<AccountShortfall>> = safeCall {
        postgrest.rpc("account_forecast", buildJsonObject { put("p_group_id", groupId) })
            .decodeList<ForecastDto>()
            .toShortfalls()
    }

    override suspend fun confirm(
        commitmentId: String,
        kind: CommitmentKind,
        dueDate: LocalDate,
        amountCents: Long,
        date: LocalDate,
        cover: List<CoverItem>,
    ): Result<ConfirmedFact> = safeCall {
        val id = postgrest.rpc(
            "confirm_occurrence",
            buildJsonObject {
                put("p_commitment_id", commitmentId)
                put("p_due_date", dueDate.toString())
                put("p_amount", amountCents)
                put("p_date", date.toString())
                cover.toCoverJson()?.let { put("p_cover", it) }
            },
        ).decodeAs<String>()
        ConfirmedFact(id, kind)
    }

    override suspend fun skip(commitmentId: String, dueDate: LocalDate): Result<Unit> = safeCall {
        postgrest.rpc(
            "skip_occurrence",
            buildJsonObject {
                put("p_commitment_id", commitmentId)
                put("p_due_date", dueDate.toString())
            },
        )
        Unit
    }

    override suspend fun voidConfirmed(fact: ConfirmedFact): Result<Unit> = safeCall {
        if (fact.kind == CommitmentKind.GroupTransferOut) {
            postgrest.rpc("void_group_transfer", buildJsonObject { put("p_link_id", fact.id) })
            return@safeCall
        }
        val table = if (fact.kind == CommitmentKind.Transfer) "account_transfers" else "transactions"
        val updated = postgrest.from(table)
            .update({ set("voided_at", Clock.System.now().toString()) }) {
                select(Columns.list("id"))
                filter { eq("id", fact.id) }
            }
            .decodeList<IdDto>()
        // Row level security hides a row from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    @Serializable
    private data class IdDto(val id: String)
}
