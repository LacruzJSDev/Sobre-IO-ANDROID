package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.export.ExportRecordType
import dev.lacruzjs.sobreio.domain.export.ExportRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseExportRepository(client: SupabaseClient) : ExportRepository {

    private val postgrest = client.postgrest

    override suspend fun exportPeriod(groupId: String, periodId: String): Result<List<ExportRow>> = safeCall {
        postgrest.rpc(
            "export_period",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_period_id", periodId)
            },
        ).decodeList<ExportRowDto>().map { it.toDomain() }
    }

    @Serializable
    private data class ExportRowDto(
        @SerialName("record_type") val recordType: String,
        val date: String,
        val kind: String,
        val amount: Long = 0,
        val account: String? = null,
        @SerialName("to_account") val toAccount: String? = null,
        val envelope: String? = null,
        @SerialName("to_envelope") val toEnvelope: String? = null,
        val notes: String? = null,
        val tags: String? = null,
        val author: String? = null,
    ) {
        fun toDomain() = ExportRow(
            recordType = when (recordType) {
                "account_transfer" -> ExportRecordType.AccountTransfer
                "envelope_transfer" -> ExportRecordType.EnvelopeTransfer
                else -> ExportRecordType.Transaction
            },
            date = LocalDate.parse(date),
            kind = kind,
            amountCents = amount,
            account = account,
            toAccount = toAccount,
            envelope = envelope,
            toEnvelope = toEnvelope,
            notes = notes,
            tags = tags,
            author = author,
        )
    }
}
