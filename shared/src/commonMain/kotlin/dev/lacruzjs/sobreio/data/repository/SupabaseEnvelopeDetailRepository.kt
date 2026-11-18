package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeOutlookDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopePeriodDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeStyleDto
import dev.lacruzjs.sobreio.data.remote.dto.EnvelopeTransferDto
import dev.lacruzjs.sobreio.data.remote.dto.FundedVersionDto
import dev.lacruzjs.sobreio.data.remote.dto.LiveCommitmentDto
import dev.lacruzjs.sobreio.data.remote.mapper.toCommitmentKind
import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.EnvelopeOutlook
import dev.lacruzjs.sobreio.domain.model.EnvelopePeriod
import dev.lacruzjs.sobreio.domain.model.EnvelopeTransferPage
import dev.lacruzjs.sobreio.domain.model.FundingCommitment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlin.time.Clock

/**
 * Reads the envelope detail from the `envelope_balances`, `envelope_periods` and `envelope_outlook_current` views and the
 * `envelope_transfers` table. The only write is voiding a move, which row level security allows to every member.
 */
class SupabaseEnvelopeDetailRepository(client: SupabaseClient) : EnvelopeDetailRepository {

    private val postgrest = client.postgrest

    override suspend fun getEnvelope(envelopeId: String): Result<Envelope> = safeCall {
        val balance = postgrest.from(BALANCES_VIEW)
            .select(Columns.raw("envelope_id,group_id,name,kind,owner_user_id,archived_at,monthly_target,balance")) {
                filter { eq("envelope_id", envelopeId) }
            }
            .decodeSingleOrNull<EnvelopeDto>()
            ?: throw AppError.NotFound
        // The balances view has no colour or icon: they come from `envelopes`
        val style = postgrest.from(ENVELOPES_TABLE)
            .select(Columns.list("id", "color", "icon", "purpose", "targets_from")) { filter { eq("id", envelopeId) } }
            .decodeSingleOrNull<EnvelopeStyleDto>()
        balance.toDomain(style) ?: throw AppError.NotFound
    }

    override suspend fun getPeriods(envelopeId: String, limit: Int): Result<List<EnvelopePeriod>> = safeCall {
        postgrest.from(PERIODS_VIEW)
            .select(Columns.raw("period_id,start_date,end_date,opening_balance,assigned,activity,closing_balance")) {
                filter { eq("envelope_id", envelopeId) }
                order("start_date", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<EnvelopePeriodDto>()
            .map { it.toDomain() }
            .reversed()
    }

    override suspend fun getOutlook(groupId: String, envelopeId: String): Result<EnvelopeOutlook?> = safeCall {
        postgrest.from(OUTLOOK_VIEW)
            .select(Columns.list("balance", "committed", "free", "shortfall")) {
                filter {
                    eq("group_id", groupId)
                    eq("envelope_id", envelopeId)
                }
            }
            .decodeSingleOrNull<EnvelopeOutlookDto>()
            ?.toDomain()
    }

    override suspend fun getTransfers(envelopeId: String, offset: Int, limit: Int): Result<EnvelopeTransferPage> = safeCall {
        val result = postgrest.from(TRANSFERS_TABLE)
            .select(Columns.raw(TRANSFER_COLUMNS)) {
                count(Count.EXACT)
                filter {
                    exact("voided_at", null)
                    or {
                        eq("from_envelope_id", envelopeId)
                        eq("to_envelope_id", envelopeId)
                    }
                }
                order("created_at", Order.DESCENDING)
                order("id", Order.ASCENDING)
                range(offset.toLong(), offset.toLong() + limit - 1)
            }
        val rows = result.decodeList<EnvelopeTransferDto>()
        // The name of a commitment is read apart, so a row never fails for the sake of its label
        val commitmentIds = rows.mapNotNull { it.commitmentId }.distinct()
        val names = if (commitmentIds.isEmpty()) {
            emptyMap()
        } else {
            postgrest.from(COMMITMENTS_TABLE)
                .select(Columns.list("id", "name")) { filter { isIn("id", commitmentIds) } }
                .decodeList<LiveCommitmentDto>()
                .associate { it.id to it.name }
        }
        val items = rows.map { it.toDomain(names[it.commitmentId]) }
        EnvelopeTransferPage(items, (result.countOrNull() ?: (offset + items.size).toLong()).toInt())
    }

    override suspend fun voidTransfer(id: String): Result<Unit> = safeCall {
        val updated = postgrest.from(TRANSFERS_TABLE)
            .update({ set("voided_at", Clock.System.now().toString()) }) {
                select(Columns.list("id"))
                filter {
                    eq("id", id)
                    isIn("origin", VOIDABLE_ORIGINS)
                }
            }
            .decodeList<IdDto>()
        // Row level security hides the row from an update that is not allowed: no error, no row
        if (updated.isEmpty()) throw AppError.BusinessRule("FZ016")
    }

    override suspend fun getFundingCommitments(envelopeId: String): Result<List<FundingCommitment>> = safeCall {
        val versions = postgrest.from(VERSIONS_TABLE)
            .select(Columns.list("commitment_id", "account_id", "kind", "effective_from")) {
                filter {
                    eq("envelope_id", envelopeId)
                    eq("funded", true)
                }
                order("effective_from", Order.DESCENDING)
            }
            .decodeList<FundedVersionDto>()
        // The newest funded version of each commitment tells which account to open it from
        val newest = versions.distinctBy { it.commitmentId }
        if (newest.isEmpty()) return@safeCall emptyList()
        val live = postgrest.from(COMMITMENTS_TABLE)
            .select(Columns.list("id", "name")) {
                filter {
                    isIn("id", newest.map { it.commitmentId })
                    exact("archived_at", null)
                }
                order("name", Order.ASCENDING)
            }
            .decodeList<LiveCommitmentDto>()
        live.mapNotNull { commitment ->
            newest.firstOrNull { it.commitmentId == commitment.id }?.let {
                FundingCommitment(commitment.id, commitment.name, it.accountId, it.kind.toCommitmentKind())
            }
        }
    }

    @Serializable
    private data class IdDto(val id: String)

    private companion object {
        const val BALANCES_VIEW = "envelope_balances"
        const val PERIODS_VIEW = "envelope_periods"
        const val OUTLOOK_VIEW = "envelope_outlook_current"
        const val ENVELOPES_TABLE = "envelopes"
        const val TRANSFERS_TABLE = "envelope_transfers"
        const val COMMITMENTS_TABLE = "commitments"
        const val VERSIONS_TABLE = "commitment_versions"
    }
}

/** The columns of `envelope_transfers` that [dev.lacruzjs.sobreio.domain.model.EnvelopeTransfer] needs. */
internal const val TRANSFER_COLUMNS = "id,from_envelope_id,to_envelope_id,amount,origin,commitment_id,created_at,notes"

/** The origins the app voids ([dev.lacruzjs.sobreio.domain.model.TransferOrigin.canVoid]); the filter keeps the rest safe. */
internal val VOIDABLE_ORIGINS = listOf("manual", "target", "commitment")
